import useEventSubscription from "./hooks/useEventSubscription";

function App() {
  const event = useEventSubscription();

  return (
    <section id="center">
      <div>
        <h1>Tick: {event.data?.tick ?? "Unknown"}</h1>
      </div>
    </section>
  );
}

export default App;
