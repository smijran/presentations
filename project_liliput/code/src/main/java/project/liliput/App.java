package project.liliput;

public class App {
    public String getGreeting() {
        return "Hello from Project Liliput!";
    }

    public static void main(String[] args) {
        System.out.println(new App().getGreeting());
    }
}
