package aeminium.fft;
import java.util.Random;

public class FFTUtility {

	public static int DEFAULT_SIZE = 1024;
	
	public static Complex[] createRandomComplexArray(Complex[] x, int n) {
			
		Random r = new Random(1L);
	
		x = new Complex[n];
		
		for (int i = 0; i < n; i++) {
		
			x[i] = new Complex(i, 0);// check for this expression 
			
			x[i] = new Complex(-2 * r.nextDouble() + 1, 0);
		}
		return x;
	}
	public static void show(Complex[] x, String title) {
		
		System.out.println("-------------------");
		
		for (int i = 0; i < x.length; i++) {
			System.out.println(x[i]);
		}
		System.out.println();
	}
}
