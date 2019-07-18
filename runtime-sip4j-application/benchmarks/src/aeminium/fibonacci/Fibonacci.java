package aeminium.fibonacci;

public class Fibonacci{

  public Integer number;

  Fibonacci(){
	  number = 10;
  }
  public Integer computeFibo(Integer num) {

	 //num = 10;

	if (num <= 1) {// Read-Only rule applied on n

		return num; // Read-Only rule applied here
    }
	else
    	return computeFibo(num-1) + computeFibo(num); // Recursive method call (Immutable-M-Call()) rule applied
  }
  public  void display(Integer num){
	  System.out.println("Number =  " +num);
  }

  public static void main(String[] args) {

	   long start = System.nanoTime(); //library function (do-nothing)

	   Fibonacci obj = new Fibonacci();

    	obj.computeFibo(obj.number);//MethodCall(<Immutable>, number)

    	obj.display(obj.number);// // MethodCall(<Immutable>, number)

    	long elapsedTime = System.nanoTime()-start;//library function (do -nothing)

    	double ms = (double) elapsedTime / 1000000.0;//library function (do -nothing)

    	System.out.println(" Milli Seconds Time = "+ms);//library function (do -nothing)
    }

}
