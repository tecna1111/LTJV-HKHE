// RGA traversal is iterative so long documents do not overflow the JS call stack.
export function visibleText(state) {
  const children = new Map();
  for (const [id, node] of Object.entries(state)) {
    const list = children.get(node.after) || [];
    list.push({ id, ...node }); children.set(node.after, list);
  }
  for (const list of children.values()) list.sort((a, b) => b.order - a.order || b.id.localeCompare(a.id));
  const stack = [...(children.get('') || [])].reverse();
  const ids = [], units = [];
  while (stack.length) {
    const node = stack.pop();
    if (!node.deleted) { ids.push(node.id); units.push(String.fromCharCode(node.unit)); }
    const list = children.get(node.id) || [];
    for (let i = list.length - 1; i >= 0; i--) stack.push(list[i]);
  }
  return { ids, text: units.join('') };
}

export function projectPending(snapshot, pending) {
  const state = structuredClone(snapshot.state);
  let order = snapshot.revision;
  for (const op of pending) {
    const p = op.payload;
    order++;
    if (op.action === 'text.edit') {
      for (const id of p.deleteIds) if (state[id]) state[id].deleted = true;
      let after = p.after;
      for (let i = 0; i < p.text.length; i++) {
        const id = `${op.operationId}:${i}`;
        state[id] ||= { after, unit: p.text.charCodeAt(i), order, deleted: false };
        after = id;
      }
    } else {
      state[p.id] = { version: state[p.id]?.version || 0, deleted: op.action === 'object.delete', value: p.value };
    }
  }
  return state;
}

export function textEdit(before, nextText, operationId) {
  let start = 0;
  while (start < before.text.length && start < nextText.length && before.text[start] === nextText[start]) start++;
  let end = before.text.length, nextEnd = nextText.length;
  while (end > start && nextEnd > start && before.text[end - 1] === nextText[nextEnd - 1]) { end--; nextEnd--; }
  const text = nextText.slice(start, nextEnd), deleteIds = before.ids.slice(start, end);
  if (!text.length && !deleteIds.length) return null;
  if (text.length > 4000 || deleteIds.length > 4000) throw new Error('Mỗi thao tác tối đa 4000 ký tự. Hãy chia nội dung thành nhiều phần.');
  return { operationId, action: 'text.edit', payload: { after: before.ids[start - 1] || '', text, deleteIds } };
}
