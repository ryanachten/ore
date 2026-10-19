import type { WebsocketsEvent } from "../models/event";
import type { Vehicle } from "../models/vehicle";

export type WorldState = {
  tick: number;
  vehicles: Record<string, Vehicle>;
};

export const initialWorldState: WorldState = {
  tick: 0,
  vehicles: {},
};

export const worldStateReducer = (state: WorldState, event: WebsocketsEvent): WorldState => {
  if (event.tick < state.tick) return state;

  switch (event.type) {
    case "sim.tick":
      return { ...state, tick: event.tick };
    case "vehicle.tick": {
      const { vehicle } = event.payload;
      return {
        tick: event.tick,
        vehicles: { ...state.vehicles, [vehicle.id]: vehicle },
      };
    }
    case "world.state": {
      const { vehicles: newVehicles } = event.payload.state;

      const vehicles = { ...state.vehicles };
      newVehicles.forEach((v) => (vehicles[v.id] = v));

      return { tick: event.tick, vehicles };
    }
    default:
      event satisfies never;
      return state;
  }
};
