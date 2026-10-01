import { act, cleanup, renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { WEBSOCKET_ENDPOINT } from "../constants/routes";
import useEventSubscription from "./useEventSubscription";

type SocketHandler = () => void;

class FakeWebSocket {
  static instances: FakeWebSocket[] = [];

  url: string;
  onopen: SocketHandler | null = null;
  onclose: SocketHandler | null = null;
  onerror: SocketHandler | null = null;
  onmessage: ((event: MessageEvent<string>) => void) | null = null;
  close = vi.fn();

  constructor(url: string) {
    this.url = url;
    FakeWebSocket.instances.push(this);
  }
}

const openedSocket = (): FakeWebSocket => {
  const socket = FakeWebSocket.instances.at(-1);
  if (socket === undefined) {
    throw new Error("No WebSocket was opened");
  }
  return socket;
};

const message = (data: string): MessageEvent<string> =>
  new MessageEvent<string>("message", { data });

const tickEnvelope = (tick: number): string =>
  JSON.stringify({
    id: "e7c5f4c0-0000-0000-0000-000000000001",
    type: "sim.tick",
    tick,
    source: "world",
    version: 1,
    payload: {},
  });

beforeEach(() => {
  FakeWebSocket.instances = [];
  vi.stubGlobal("WebSocket", FakeWebSocket);
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe("useEventSubscription", () => {
  it("starts disconnected with no envelope", () => {
    const { result } = renderHook(() => useEventSubscription());

    expect(result.current.data).toBeNull();
    expect(result.current.isConnected).toBe(false);
    expect(result.current.hasError).toBe(false);
  });

  it("connects to the proxied endpoint rather than an absolute gateway url", () => {
    renderHook(() => useEventSubscription());

    expect(openedSocket().url).toBe(WEBSOCKET_ENDPOINT);
    expect(openedSocket().url).toBe("/ws");
  });

  it("reports connected once the socket opens", () => {
    const { result } = renderHook(() => useEventSubscription());

    act(() => openedSocket().onopen?.());

    expect(result.current.isConnected).toBe(true);
  });

  it("reports disconnected once the socket closes", () => {
    const { result } = renderHook(() => useEventSubscription());
    const socket = openedSocket();

    act(() => socket.onopen?.());
    act(() => socket.onclose?.());

    expect(result.current.isConnected).toBe(false);
  });

  it("exposes the envelope for a tick message", () => {
    const { result } = renderHook(() => useEventSubscription());

    act(() => openedSocket().onmessage?.(message(tickEnvelope(42))));

    expect(result.current.data?.tick).toBe(42);
    expect(result.current.data?.source).toBe("world");
  });

  it("ignores messages that are not tick envelopes", () => {
    const { result } = renderHook(() => useEventSubscription());

    act(() => openedSocket().onmessage?.(message("not-an-envelope")));

    expect(result.current.data).toBeNull();
  });

  it("ignores json that is not an envelope", () => {
    const { result } = renderHook(() => useEventSubscription());

    act(() => openedSocket().onmessage?.(message(JSON.stringify({ hello: "world" }))));

    expect(result.current.data).toBeNull();
  });

  it("flags an error when the socket fails", () => {
    const { result } = renderHook(() => useEventSubscription());

    act(() => openedSocket().onerror?.());

    expect(result.current.hasError).toBe(true);
  });

  it("closes the socket when unmounted", () => {
    const { unmount } = renderHook(() => useEventSubscription());
    const socket = openedSocket();

    unmount();

    expect(socket.close).toHaveBeenCalled();
  });
});
