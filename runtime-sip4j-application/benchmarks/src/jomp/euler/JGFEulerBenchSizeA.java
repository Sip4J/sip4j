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


package jomp.euler; 
import jomp.jgfutil.JGFInstrumentor;

public class JGFEulerBenchSizeA{ 

  public static void main(String argv[]){

  //  JGFInstrumentor.printHeader(3,0,jomp.runtime.OMP.getMaxThreads());

    JGFEulerBench eb = new JGFEulerBench(); 
    
    eb.JGFrun(0);
 
  }
}
 
