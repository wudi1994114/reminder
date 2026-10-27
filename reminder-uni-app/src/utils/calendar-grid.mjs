// Render only the complete weeks that intersect the displayed month.
export function monthGridDates(year, month) {
  const first = new Date(year, month - 1, 1);
  const dayCount = new Date(year, month, 0).getDate();
  const cellCount = Math.ceil((first.getDay() + dayCount) / 7) * 7;
  const start = new Date(first);
  start.setDate(1 - first.getDay());
  return Array.from({ length: cellCount }, (_, index) => {
    const date = new Date(start);
    date.setDate(start.getDate() + index);
    return date;
  });
}
