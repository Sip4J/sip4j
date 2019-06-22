package jomp.montecarlo2;
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


import jomp.jgfutil.*;
import java.io.*;


public class JGFMonteCarloBenchSizeA{ 

 public static void main(String argv[]){

    JGFMonteCarloBench mc = new JGFMonteCarloBench(); 
    
    mc.JGFrun(0);
    /*int size = 0;
    //JGFInstrumentor.startTimer("Section3:MonteCarlo:Total");

    JGFInstrumentor.addTimer("Section3:MonteCarlo:Total", "Solutions",size);
    //JGFInstrumentor.addTimer("Section3:MonteCarlo:Run", "Samples",size);

    mc.JGFsetsize(size); 

  
    mc.JGFinitialise(); 
    mc.JGFapplication(); 
    mc.JGFvalidate(); 
    mc.JGFtidyup(); 

   //JGFInstrumentor.stopTimer("Section3:MonteCarlo:Total");
    
    //JGFInstrumentor.addOpsToTimer("Section3:MonteCarlo:Run", (double) input[1] );
    JGFInstrumentor.addOpsToTimer("Section3:MonteCarlo:Total", 1);

    JGFInstrumentor.printTimer("Section3:MonteCarlo:Run"); 
    JGFInstrumentor.printTimer("Section3:MonteCarlo:Total"); */
 
  }
}
 
