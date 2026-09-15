package com.cosre.cosre_backend.modules.kanban.service;
import com.cosre.cosre_backend.modules.kanban.entity.KanbanBoard;
import com.cosre.cosre_backend.modules.kanban.repository.KanbanBoardRepository;
import com.cosre.cosre_backend.modules.kanban.entity.KanbanEvent;
import com.cosre.cosre_backend.modules.kanban.repository.KanbanEventRepository;
import com.cosre.cosre_backend.modules.kanban.entity.KanbanSprint;
import com.cosre.cosre_backend.modules.kanban.repository.KanbanSprintRepository;
import com.cosre.cosre_backend.modules.kanban.entity.KanbanSubtask;
import com.cosre.cosre_backend.modules.kanban.repository.KanbanSubtaskRepository;
import com.cosre.cosre_backend.modules.kanban.entity.KanbanTask;
import com.cosre.cosre_backend.modules.kanban.repository.KanbanTaskRepository;

import com.cosre.cosre_backend.common.exception.*;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.classroom.repository.ClassroomRepository;
import com.cosre.cosre_backend.modules.checkpoint.repository.CheckpointRepository;
import com.cosre.cosre_backend.modules.project.repository.ProjectMilestoneRepository;
import com.cosre.cosre_backend.modules.team.entity.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.LocalDateTime;
import java.util.*;
import static com.cosre.cosre_backend.modules.kanban.dto.KanbanDto.*;

@Service @RequiredArgsConstructor @Transactional
public class KanbanService {
    private final KanbanAccess access;
    private final KanbanBoardRepository boards;
    private final KanbanTaskRepository tasks;
    private final KanbanSprintRepository sprints;
    private final KanbanSubtaskRepository subtasks;
    private final KanbanEventRepository events;
    private final CheckpointRepository checkpoints;
    private final ProjectMilestoneRepository milestones;
    private final ClassroomRepository classrooms;

    private record Context(Team team, User actor, KanbanBoard board) {}
    private Context context(Long teamId,String username) {
        User actor=access.actor(username); Team team=access.view(teamId,actor);
        KanbanBoard board=boards.findByTeamId(teamId).orElseGet(()->{
            var b=new KanbanBoard(); b.setTeamId(teamId); return boards.saveAndFlush(b);
        });
        return new Context(team,actor,board);
    }
    private void revision(Context c, Long expected) {
        if(expected==null || expected!=c.board.getRevision())
            throw new DuplicateResourceException("Board đã thay đổi. Tải lại trước khi lưu.");
    }
    private KanbanTask task(Context c,Long id) {
        return tasks.findById(id).filter(t->t.getBoardId().equals(c.board.getId()))
            .orElseThrow(()->new ResourceNotFoundException("Task không thuộc board này"));
    }
    private KanbanSprint sprint(Context c,Long id) {
        return sprints.findById(id).filter(s->s.getBoardId().equals(c.board.getId()))
            .orElseThrow(()->new ResourceNotFoundException("Sprint không thuộc board này"));
    }
    private void event(Context c,String action,String detail) {
        c.board.setRevision(c.board.getRevision()+1);
        var e=new KanbanEvent(); e.setBoardId(c.board.getId()); e.setActorId(c.actor.getId());
        e.setAction(action); e.setDetail(detail); e.setOccurredAt(LocalDateTime.now()); events.save(e);
    }
    public BoardView board(Long teamId,String username) { return snapshot(context(teamId,username)); }
    public Metrics metrics(Long teamId,String username) {
        var c=context(teamId,username); return calculate(c.team,tasks.findByBoardIdOrderByPositionAscIdAsc(c.board.getId()));
    }
    public void deleteBoard(Long teamId,String username) {
        var actor=access.actor(username);
        access.view(teamId,actor);
        if(actor.getRole()!=com.cosre.cosre_backend.common.constants.RoleEnum.LECTURER)
            throw new org.springframework.security.access.AccessDeniedException("Only the managing lecturer can delete the board");
        boards.findByTeamId(teamId).ifPresent(board -> {
            var all=tasks.findByBoardIdOrderByPositionAscIdAsc(board.getId());
            for(var t:all) subtasks.deleteByTaskId(t.getId());
            tasks.deleteAll(all); tasks.flush();
            sprints.deleteAll(sprints.findByBoardIdOrderByStartsOnAsc(board.getId())); sprints.flush();
            events.deleteByBoardId(board.getId()); events.flush();
            boards.delete(board); boards.flush();
        });
    }
    public BoardView saveTask(Long teamId,Long taskId,TaskInput input,String username) {
        var c=context(teamId,username); revision(c,input.revision());
        var t=taskId==null?new KanbanTask():task(c,taskId);
        String before=taskId==null?"new":audit(t);
        if(input.assigneeIds()==null || input.assigneeIds().isEmpty())
            throw new BusinessRuleException("Task cần ít nhất một người được giao");
        var classroom=classrooms.findDetailedById(c.team.getClassroomId()).orElseThrow();
        for(Long id:input.assigneeIds()) {
            boolean valid=c.team.getMembers().stream().anyMatch(u->u.getId().equals(id) && u.isActive())
                && classroom.getStudents().stream().anyMatch(u->u.getId().equals(id));
            if(!valid) throw new BusinessRuleException("Người được giao phải là thành viên đang hoạt động trong nhóm và lớp");
        }
        if(input.weight()==null || input.weight().signum()<=0) throw new BusinessRuleException("Trọng số phải lớn hơn 0");
        Long milestoneId=input.milestoneId();
        if(input.checkpointId()!=null) {
            var cp=checkpoints.findById(input.checkpointId()).orElseThrow(()->new ResourceNotFoundException("Checkpoint không tồn tại"));
            if(!Objects.equals(cp.getTeamId(),teamId)) throw new BusinessRuleException("Checkpoint phải thuộc cùng nhóm");
            if(milestoneId!=null && !Objects.equals(cp.getMilestoneId(),milestoneId))
                throw new BusinessRuleException("Milestone không khớp checkpoint");
            milestoneId=cp.getMilestoneId();
        }
        if(milestoneId!=null) {
            var m=milestones.findById(milestoneId).orElseThrow(()->new ResourceNotFoundException("Milestone không tồn tại"));
            if(!Objects.equals(m.getProject().getId(),c.team.getProjectId()))
                throw new BusinessRuleException("Milestone phải thuộc dự án của nhóm");
        }
        if(input.sprintId()!=null) sprint(c,input.sprintId());
        if(taskId==null) {
            t.setBoardId(c.board.getId()); t.setCreatedBy(c.actor.getId());
            t.setPosition(tasks.findByBoardIdOrderByPositionAscIdAsc(c.board.getId()).size());
        }
        t.setTitle(input.title().trim()); t.setDescription(input.description()); t.setWeight(input.weight());
        t.setDueOn(input.dueOn()); t.setSprintId(input.sprintId()); t.setMilestoneId(milestoneId);
        t.setCheckpointId(input.checkpointId()); t.getAssigneeIds().clear(); t.getAssigneeIds().addAll(input.assigneeIds());
        tasks.saveAndFlush(t); event(c,taskId==null?"TASK_CREATED":"TASK_UPDATED",before+" -> "+audit(t));
        return snapshot(c);
    }
    public BoardView move(Long teamId,Long id,MoveInput input,String username) {
        var c=context(teamId,username); revision(c,input.revision()); var t=task(c,id);
        if(input.status()==KanbanTask.Status.DONE && subtasks.findByTaskIdOrderByIdAsc(id).stream().anyMatch(s->!s.isDone()))
            throw new BusinessRuleException("Hoàn thành các subtask trước khi chuyển Done");
        String before=audit(t); t.setStatus(input.status());
        var column=new ArrayList<>(tasks.findByBoardIdOrderByPositionAscIdAsc(c.board.getId()).stream()
            .filter(x->x.getStatus()==input.status() && !x.getId().equals(id)).toList());
        if(input.position()>column.size() || input.position()<0) throw new BusinessRuleException("Vị trí trên board không hợp lệ");
        column.add(input.position(),t); for(int i=0;i<column.size();i++) column.get(i).setPosition(i);
        event(c,"TASK_MOVED",before+" -> "+audit(t)); return snapshot(c);
    }
    public BoardView deleteTask(Long teamId,Long id,Long rev,String username) {
        var c=context(teamId,username); revision(c,rev); var t=task(c,id);
        event(c,"TASK_DELETED",audit(t)); subtasks.deleteByTaskId(id); tasks.delete(t); tasks.flush(); return snapshot(c);
    }
    public BoardView saveSprint(Long teamId,Long id,SprintInput input,String username) {
        var c=context(teamId,username); access.requireManager(c.team,c.actor); revision(c,input.revision());
        if(input.endsOn().isBefore(input.startsOn())) throw new BusinessRuleException("Ngày kết thúc phải từ ngày bắt đầu trở đi");
        var s=id==null?new KanbanSprint():sprint(c,id);
        String before=id==null?"new":s.getTitle()+" "+s.getStartsOn()+".."+s.getEndsOn();
        s.setBoardId(c.board.getId()); s.setTitle(input.title().trim()); s.setStartsOn(input.startsOn()); s.setEndsOn(input.endsOn());
        sprints.saveAndFlush(s); event(c,"SPRINT_SAVED",s.getId()+": "+before+" -> "+s.getTitle()+" "+s.getStartsOn()+".."+s.getEndsOn());
        return snapshot(c);
    }
    public BoardView deleteSprint(Long teamId,Long id,Long rev,String username) {
        var c=context(teamId,username); access.requireManager(c.team,c.actor); revision(c,rev); var s=sprint(c,id);
        for(var t:tasks.findByBoardIdOrderByPositionAscIdAsc(c.board.getId())) if(Objects.equals(t.getSprintId(),id)) {
            t.setSprintId(null); event(c,"TASK_UNSCHEDULED","Task #"+t.getId()+" bỏ liên kết sprint #"+id);
        }
        tasks.flush(); sprints.delete(s); sprints.flush(); event(c,"SPRINT_DELETED",id+": "+s.getTitle()); return snapshot(c);
    }
    public BoardView saveSubtask(Long teamId,Long taskId,Long id,SubtaskInput input,String username) {
        var c=context(teamId,username); revision(c,input.revision()); var t=task(c,taskId);
        var s=id==null?new KanbanSubtask():subtasks.findById(id).filter(x->x.getTaskId().equals(taskId))
            .orElseThrow(()->new ResourceNotFoundException("Subtask không thuộc task"));
        String before=id==null?"new":s.getTitle()+" done="+s.isDone();
        s.setTaskId(taskId); s.setTitle(input.title().trim()); s.setDone(input.done()); subtasks.saveAndFlush(s);
        if(!s.isDone() && t.getStatus()==KanbanTask.Status.DONE) {
            t.setStatus(KanbanTask.Status.IN_PROGRESS); event(c,"TASK_REOPENED","Task #"+taskId+" có subtask chưa xong");
        }
        event(c,"SUBTASK_SAVED","Task #"+taskId+", subtask #"+s.getId()+": "+before+" -> "+s.getTitle()+" done="+s.isDone());
        return snapshot(c);
    }
    public BoardView deleteSubtask(Long teamId,Long taskId,Long id,Long rev,String username) {
        var c=context(teamId,username); revision(c,rev); task(c,taskId);
        var s=subtasks.findById(id).filter(x->x.getTaskId().equals(taskId)).orElseThrow(()->new ResourceNotFoundException("Subtask không thuộc task"));
        event(c,"SUBTASK_DELETED","Task #"+taskId+", subtask #"+id+": "+s.getTitle()); subtasks.delete(s); subtasks.flush(); return snapshot(c);
    }
    private String audit(KanbanTask t) {
        return "#"+t.getId()+" "+t.getTitle()+" status="+t.getStatus()+" position="+t.getPosition()+" weight="+t.getWeight()
            +" assignees="+new TreeSet<>(t.getAssigneeIds())+" due="+t.getDueOn()+" sprint="+t.getSprintId()
            +" milestone="+t.getMilestoneId()+" checkpoint="+t.getCheckpointId()+" description="
            +(t.getDescription()==null?"":t.getDescription().substring(0,Math.min(1000,t.getDescription().length())));
    }
    private BoardView snapshot(Context c) {
        var all=tasks.findByBoardIdOrderByPositionAscIdAsc(c.board.getId());
        var ms=c.team.getProjectId()==null?List.<Choice>of():milestones.findByProjectIdOrderByDisplayOrderAsc(c.team.getProjectId()).stream()
            .map(m->new Choice(m.getId(),m.getTitle(),null)).toList();
        var cps=checkpoints.findByTeamIdOrderByDueAtAsc(c.team.getId()).stream()
            .map(cp->new Choice(cp.getId(),cp.getTitle(),cp.getMilestoneId())).toList();
        return new BoardView(c.board.getId(),c.team.getId(),c.team.getName(),c.board.getRevision(),access.manager(c.team,c.actor),true,
            c.team.getMembers().stream().map(u->new Person(u.getId(),u.getFullName())).toList(),ms,cps,
            sprints.findByBoardIdOrderByStartsOnAsc(c.board.getId()).stream().map(s->new SprintView(s.getId(),s.getTitle(),s.getStartsOn(),s.getEndsOn())).toList(),
            all.stream().map(t->new TaskView(t.getId(),t.getTitle(),t.getDescription(),t.getStatus(),t.getWeight(),t.getDueOn(),t.getPosition(),t.getSprintId(),t.getMilestoneId(),t.getCheckpointId(),Set.copyOf(t.getAssigneeIds()),
                subtasks.findByTaskIdOrderByIdAsc(t.getId()).stream().map(s->new SubtaskView(s.getId(),s.getTitle(),s.isDone())).toList())).toList(),
            calculate(c.team,all),events.findTop100ByBoardIdOrderByIdDesc(c.board.getId()).stream().map(e->new EventView(e.getId(),e.getActorId(),e.getAction(),e.getDetail(),e.getOccurredAt())).toList());
    }
    static Metrics calculate(Team team,List<KanbanTask> all) {
        BigDecimal total=BigDecimal.ZERO, done=BigDecimal.ZERO; Map<Long,BigDecimal> shares=new TreeMap<>();
        for(var u:team.getMembers()) shares.put(u.getId(),BigDecimal.ZERO);
        for(var t:all) {
            total=total.add(t.getWeight()); if(t.getStatus()!=KanbanTask.Status.DONE) continue;
            done=done.add(t.getWeight()); if(t.getAssigneeIds().isEmpty()) continue;
            var share=t.getWeight().divide(BigDecimal.valueOf(t.getAssigneeIds().size()),12,RoundingMode.HALF_UP);
            for(Long id:t.getAssigneeIds()) shares.merge(id,share,BigDecimal::add);
        }
        final BigDecimal denominator=done;
        var contributions=shares.entrySet().stream().map(e->new Contribution(e.getKey(),team.getMembers().stream()
            .filter(u->u.getId().equals(e.getKey())).map(User::getFullName).findFirst().orElse("Thành viên cũ #"+e.getKey()),
            e.getValue().setScale(4,RoundingMode.HALF_UP),percent(e.getValue(),denominator))).toList();
        return new Metrics("PREVIEW_PENDING_APPROVAL","Xem trước: chia đều trọng số task Done cho người được giao; không phải điểm học tập.",total,done,percent(done,total),contributions);
    }
    private static BigDecimal percent(BigDecimal value,BigDecimal total) {
        return total.signum()==0?BigDecimal.ZERO:value.multiply(BigDecimal.valueOf(100)).divide(total,2,RoundingMode.HALF_UP);
    }
}
