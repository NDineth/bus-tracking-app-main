export const colomboDate = (date = new Date()) => new Date(date.getTime() + 19800000).toISOString().slice(0, 10);
