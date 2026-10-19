import { describe, expect, it } from "vitest";

import type { CurrentWorldStateEvent, SimTickEvent, VehicleTickEvent } from "../models/event";
import type { Vehicle } from "../models/vehicle";

import { initialWorldState, worldStateReducer } from "./worldStateReducer";

const simTick = (tick: number): SimTickEvent => ({
  id: `sim-tick-${tick}`,
  type: "sim.tick",
  tick,
  source: "world",
  version: 1,
  payload: undefined,
});

const vehicleTick = (tick: number, vehicle: Vehicle): VehicleTickEvent => ({
  id: `vehicle-tick-${tick}-${vehicle.id}`,
  type: "vehicle.tick",
  tick,
  source: "world",
  version: 1,
  payload: { vehicle },
});

const currentWorldState = (tick: number, vehicles: Vehicle[]): CurrentWorldStateEvent => ({
  id: `current-world-state-${tick}`,
  type: "world.state",
  tick,
  source: "world",
  version: 1,
  payload: { state: { tick, vehicles } },
});

const vehicle = (id: string, x: number): Vehicle => ({
  id,
  state: "IDLE",
  position: { x, y: 0 },
});

describe("worldStateReducer", () => {
  it("starts from the initial world state", () => {
    expect(initialWorldState).toEqual({ tick: 0, vehicles: {} });
  });

  it("advances the tick on sim.tick", () => {
    const state = worldStateReducer(initialWorldState, simTick(7));

    expect(state.tick).toBe(7);
    expect(state.vehicles).toEqual({});
  });

  it("adds a vehicle on vehicle.tick", () => {
    const truck = vehicle("truck-1", 10);

    const state = worldStateReducer(initialWorldState, vehicleTick(3, truck));

    expect(state.tick).toBe(3);
    expect(state.vehicles).toEqual({ "truck-1": truck });
  });

  it("merges vehicles across vehicle.tick events", () => {
    const truck = vehicle("truck-1", 10);
    const car = vehicle("car-1", 20);

    const state = worldStateReducer(
      worldStateReducer(initialWorldState, vehicleTick(3, truck)),
      vehicleTick(4, car),
    );

    expect(state.tick).toBe(4);
    expect(state.vehicles).toEqual({ "truck-1": truck, "car-1": car });
  });

  it("merges a world.state snapshot over existing vehicles", () => {
    const stale = vehicle("truck-1", 10);
    const refreshed = vehicle("truck-1", 99);
    const car = vehicle("car-1", 20);

    const state = worldStateReducer(
      worldStateReducer(initialWorldState, vehicleTick(3, stale)),
      currentWorldState(5, [refreshed, car]),
    );

    expect(state.tick).toBe(5);
    expect(state.vehicles).toEqual({ "truck-1": refreshed, "car-1": car });
  });

  it("skips events older than the current tick", () => {
    const current = worldStateReducer(initialWorldState, simTick(10));

    expect(worldStateReducer(current, simTick(9))).toEqual(current);
    expect(worldStateReducer(current, vehicleTick(2, vehicle("truck-1", 10)))).toEqual(current);
  });

  it("keeps the tick when advancing with a vehicle.tick", () => {
    const state = worldStateReducer(initialWorldState, vehicleTick(12, vehicle("truck-1", 1)));

    expect(state.tick).toBe(12);
  });
});
