import test from 'node:test';
import assert from 'node:assert/strict';
import { projectPending, textEdit, visibleText } from './collaborationModel.js';

const edit = (operationId, after, text, deleteIds = []) => ({ operationId, action: 'text.edit', payload: { after, text, deleteIds } });
test('two concurrent inserts at one anchor retain both texts and converge', () => {
  const state = projectPending({ state: {}, revision: 0 }, [edit('a', '', 'Nam'), edit('b', '', 'Duy')]);
  assert.equal(visibleText(state).text, 'DuyNam');
  assert.equal(visibleText(Object.fromEntries(Object.entries(state).reverse())).text, 'DuyNam');
});
test('delete of an anchor does not delete a concurrent insertion', () => {
  const state = projectPending({ state: {}, revision: 0 }, [edit('a', '', 'AB'), edit('b', 'a:0', 'X'), edit('c', '', '', ['a:0'])]);
  assert.equal(visibleText(state).text, 'XB');
});
test('retry projection does not duplicate acknowledged text', () => {
  const op = edit('a', '', 'hello');
  const state = projectPending({ state: {}, revision: 0 }, [op]);
  assert.equal(visibleText(projectPending({ state, revision: 1 }, [op])).text, 'hello');
});
test('UTF16 offsets preserve Vietnamese and emoji replacement', () => {
  const state = projectPending({ state: {}, revision: 0 }, [edit('a', '', 'Chào 😀 Nam')]);
  const op = textEdit(visibleText(state), 'Chào 🌻 Nam', 'b');
  assert.equal(visibleText(projectPending({ state, revision: 1 }, [op])).text, 'Chào 🌻 Nam');
});
test('concurrent identical letters remain two independent user edits', () => {
  const state = projectPending({ state: {}, revision: 0 }, [edit('a', '', 'x'), edit('b', '', 'x')]);
  assert.equal(visibleText(state).text, 'xx');
});
test('large paste is rejected before losing local draft', () => {
  assert.throws(() => textEdit({ text: '', ids: [] }, 'x'.repeat(4001), 'a'), /4000/);
});
test('long documents use iterative traversal', () => {
  let state = {};
  for (let i = 0; i < 20; i++) state = projectPending({ state, revision: i }, [edit(`a${i}`, i ? `a${i - 1}:999` : '', 'x'.repeat(1000))]);
  assert.equal(visibleText(state).text.length, 20000);
});
