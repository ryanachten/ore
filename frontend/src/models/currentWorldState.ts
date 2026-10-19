import type { Vehicle } from "./vehicle";

export type CurrentWorldState = {
  tick: number;
  vehicles: Vehicle[];
};
