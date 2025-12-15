package project.liliput;

public class App {

	private final static class SomeObject {
	}

	private final static int NUM_OF_OBJECTS = 1000;

	public static void main() {
		int[] address = new int[NUM_OF_OBJECTS];
		for (int i = 0; i < NUM_OF_OBJECTS; i++) {
			address[i] = System.identityHashCode(new SomeObject());
		}
		for (int i = 0; i < NUM_OF_OBJECTS; i++) {
			System.out.printf("Object %d at addr: 0x%08X\n", i, address[i]);
		}
        int size = (address[NUM_OF_OBJECTS - 1] - address[NUM_OF_OBJECTS - 2]);
        System.out.println("Estimated size of object: " + size);
        System.out.println("Estimated size of all object: " + size * NUM_OF_OBJECTS);

	}
  

}
