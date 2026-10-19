import type { CurrentWorldState } from "./currentWorldState";
import type { Vehicle } from "./vehicle";

type EventType = "sim.tick" | "vehicle.tick" | "world.state";

type EventEnvelope<T extends EventType, P extends Record<string, unknown> | undefined> = {
  id: string;
  type: T;
  tick: number;
  source: string;
  version: number;
  payload: P;
};

export type SimTickEvent = EventEnvelope<"sim.tick", undefined>;
export type VehicleTickEvent = EventEnvelope<"vehicle.tick", { vehicle: Vehicle }>;
export type CurrentWorldStateEvent = EventEnvelope<"world.state", { state: CurrentWorldState }>;

export type WebsocketsEvent = SimTickEvent | VehicleTickEvent | CurrentWorldStateEvent;
