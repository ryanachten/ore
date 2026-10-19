import type { Position } from "./position";

export const VehicleStates = {
  IDLE: "IDLE",
  TRAVELING: "TRAVELING",
} as const;

type VehicleState = (typeof VehicleStates)[keyof typeof VehicleStates];

export type Vehicle = {
  id: string;
  state: VehicleState;
  position: Position;
};
