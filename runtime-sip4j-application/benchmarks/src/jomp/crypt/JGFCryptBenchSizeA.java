package jomp.crypt;
/**************************************************************************
*                                                                         *
*             Java Grande Forum Benchmark Suite - Version 2.0             *
*                                                                         *
*                            produced by                                  *
*                                                                         *
*                  Java Grande Benchmarking Project                       *
*                                                                         *
*                                at                                       *
*                                                                         *
*                Edinburgh Parallel Computing Centre                      *
*                                                                         * 
*                email: epcc-javagrande@epcc.ed.ac.uk                     *
*                                                                         *
*                                                                         *
*      This version copyright (c) The University of Edinburgh, 1999.      *
*                         All rights reserved.                            *
*                                                                         *
**************************************************************************/


import jomp.jgfutil.JGFInstrumentor;

public class JGFCryptBenchSizeA{ 

  public static void main(String argv[]){

    //JGFInstrumentor.printHeader(2,0);
    long start = System.nanoTime();// do -nothin
    JGFCryptBench cb = new JGFCryptBench(); 
    cb.JGFrun(0);
    long end = System.nanoTime();
	long elapsedTime = end - start;
	double ms = (double)elapsedTime / 1000000.0;
	System.out.println(" Milli Seconds Time = "+ms);
 
  }
}
