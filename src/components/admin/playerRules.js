function blank(value) {
  return value == null || String(value).trim() === "";
}

function issue(field, code, message, step) {
  return { field, code, message, step };
}

export function profileIssues(form, { creating, executive, hasPhoto }) {
  const rows = [];
  const need = (field, value, step) => {
    if (creating && blank(value)) rows.push(issue(field, "REQUIRED", "Required", step));
  };
  need("fullName", form.fullName, 0);
  need("dateOfBirth", form.dateOfBirth, 0);
  need("position", form.position, 0);
  need("jersey", form.jersey, 0);
  need("nationality", form.nationality, 0);
  need("foot", form.foot, 0);
  need("heightCm", form.heightCm, 0);
  need("weightKg", form.weightKg, 0);
  need("status", form.status, 0);
  if (creating && !hasPhoto) rows.push(issue("photo", "REQUIRED", "Required", 0));
  if (form.dateOfBirth) {
    const born = new Date(form.dateOfBirth);
    const now = new Date();
    let years = now.getFullYear() - born.getFullYear();
    const month = now.getMonth() - born.getMonth();
    if (month < 0 || (month === 0 && now.getDate() < born.getDate())) years -= 1;
    if (Number.isNaN(years) || years < 14 || years > 50) rows.push(issue("dateOfBirth", "AGE", "14–50", 0));
  }
  need("phone", form.phone, 1);
  need("email", form.email, 1);
  need("preferredLanguage", form.preferredLanguage, 1);
  need("address", form.address, 1);
  need("emergencyName", form.emergencyName, 1);
  need("emergencyRelationship", form.emergencyRelationship, 1);
  need("emergencyPhone", form.emergencyPhone, 1);
  if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) rows.push(issue("email", "EMAIL", "Email", 1));
  if (creating && executive) {
    need("contractStart", form.contractStart, 2);
    need("contractEnd", form.contractEnd, 2);
    need("seasonalPay", form.seasonalPay, 2);
    need("advance", form.advance, 2);
    need("releaseClause", form.releaseClause, 2);
    need("marketValue", form.marketValue, 2);
    if (!form.sfExempt) need("socialFund", form.socialFund, 2);
  }
  if (form.contractStart && form.contractEnd && form.contractEnd <= form.contractStart) {
    rows.push(issue("contractEnd", "DATE_ORDER", "End after start", 2));
  }
  if (form.seasonalPay !== "" && form.advance !== "" && Number(form.advance) > Number(form.seasonalPay)) {
    rows.push(issue("advance", "ADVANCE", "Advance too high", 2));
  }
  return rows;
}

export function paySummary({ seasonalPay, advance, socialFund, sfExempt }) {
  const seasonal = Number(seasonalPay || 0);
  const paidAdvance = Number(advance || 0);
  const social = sfExempt ? 0 : Number(socialFund || 12000);
  const monthly = Math.round(seasonal / 8);
  const three = monthly * 3;
  const net3 = three - social;
  const envelope = Math.max(0, net3 - paidAdvance);
  const owed = Math.max(0, paidAdvance - net3);
  const remaining = seasonal - three;
  const balance = remaining - social;
  return { monthly, three, social1: social, net3, envelope, owed, remaining, social2: social, totalSocial: social * 2, balance };
}

export const BONUS_TYPES = ["MATCH_WIN", "CLEAN_SHEET", "ASSIST", "GOAL", "MAN_OF_THE_MATCH", "APPEARANCE", "STARTING_XI", "MATCH_DRAW", "CUSTOM"];
