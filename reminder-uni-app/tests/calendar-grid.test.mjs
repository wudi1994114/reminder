import test from 'node:test';
import assert from 'node:assert/strict';
import { monthGridDates } from '../src/utils/calendar-grid.mjs';

test('a Sunday-starting 28-day month uses four weeks without a blank trailing week', () => {
  const dates = monthGridDates(2026, 2);
  assert.equal(dates.length, 28);
  assert.equal(dates[0].getDate(), 1);
  assert.equal(dates.at(-1).getDate(), 28);
});

test('October 2026 uses five weeks, including only necessary adjacent-month days', () => {
  const dates = monthGridDates(2026, 10);
  assert.equal(dates.length, 35);
  assert.deepEqual([dates[0].getMonth() + 1, dates[0].getDate()], [9, 27]);
  assert.deepEqual([dates.at(-1).getMonth() + 1, dates.at(-1).getDate()], [10, 31]);
});

test('six-week and leap-year months retain every day in chronological local-date order', () => {
  for (const [year, month, length, count] of [[2026, 8, 42, 31], [2028, 2, 35, 29]]) {
    const dates = monthGridDates(year, month);
    assert.equal(dates.length, length);
    assert.equal(dates[0].getDay(), 0);
    assert.equal(dates.at(-1).getDay(), 6);
    assert.equal(dates.filter(date => date.getMonth() + 1 === month).length, count);
    assert.equal(new Set(dates.map(date => date.toDateString())).size, length);
    assert.ok(dates.every(date => date.getHours() === 0));
  }
});
