export type EventEnvelope = {
    id: string,
    type: string,
    tick: number,
    source: string,
    version: number,
    payload: Record<string, unknown>
}