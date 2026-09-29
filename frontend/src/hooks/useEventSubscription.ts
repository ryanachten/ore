import { useEffect, useState } from "react"
import type { EventEnvelope } from "../models/event";

const WEBSOCKET_ENDPOINT = "ws://localhost:8080/ws";

const useEventSubscription = () => {
    const [eventEnvelope, setEventEnvelope] = useState<EventEnvelope | null>(null)

    useEffect(() => {
        const websocket = new WebSocket(WEBSOCKET_ENDPOINT);
        websocket.onopen = () => {
            console.log('Event websocket connected');
        }

        websocket.onclose = () => {
            console.log('Event websocket closed');
        }

        websocket.onerror = (e) => {
            console.log(`Error from event websocket: ${e}`);
        }

        websocket.onmessage = (event) => {
            const data: EventEnvelope = JSON.parse(event.data);
            setEventEnvelope(data);
        }

        return () => {
            websocket.close();
        }
    }, []);

    return eventEnvelope;
}

export default useEventSubscription;