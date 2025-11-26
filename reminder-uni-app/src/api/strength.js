import { request } from './http.js';

const base = {
  exercises: '/exercises',
  plans: '/workout-plans'
};

export const strengthApi = {
  // Exercises
  listExercises(params = {}) {
    const query = [];
    if (params.keyword) query.push(`keyword=${encodeURIComponent(params.keyword)}`);
    if (params.muscleGroup) query.push(`muscleGroup=${encodeURIComponent(params.muscleGroup)}`);
    const q = query.length ? `?${query.join('&')}` : '';
    return request({ url: base.exercises + q, method: 'GET' });
  },
  getExercise(id) {
    return request({ url: `${base.exercises}/${id}`, method: 'GET' });
  },
  createExercise(data) {
    return request({ url: base.exercises, method: 'POST', data });
  },
  updateExercise(id, data) {
    return request({ url: `${base.exercises}/${id}`, method: 'PUT', data });
  },
  deleteExercise(id) {
    return request({ url: `${base.exercises}/${id}`, method: 'DELETE' });
  },

  // Workout Plans
  listPlans(params = {}) {
    const query = [];
    if (params.keyword) query.push(`keyword=${encodeURIComponent(params.keyword)}`);
    const q = query.length ? `?${query.join('&')}` : '';
    return request({ url: base.plans + q, method: 'GET' });
  },
  getPlan(id) {
    return request({ url: `${base.plans}/${id}`, method: 'GET' });
  },
  createPlan(planWithItems) {
    // planWithItems: { plan: {...}, items: [...] }
    return request({ url: base.plans, method: 'POST', data: planWithItems });
  },
  updatePlan(id, planWithItems) {
    return request({ url: `${base.plans}/${id}`, method: 'PUT', data: planWithItems });
  },
  deletePlan(id) {
    return request({ url: `${base.plans}/${id}`, method: 'DELETE' });
  }
};

export default strengthApi;


