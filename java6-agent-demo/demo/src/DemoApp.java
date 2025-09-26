public class DemoApp {
    public static void main(String[] args) throws Exception {
        // Load some classes to demonstrate recording
        System.out.println("Hello from DemoApp");
        // Cause some common non-JDK classes to load if present (none here by default)
        // Sleep briefly to allow agent to capture
        Thread.sleep(500);
        System.out.println("Demo done.");
    }
}
