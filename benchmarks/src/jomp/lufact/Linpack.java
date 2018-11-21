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
*            See below for the previous history of this code              *
*                                                                         *
*      This version copyright (c) The University of Edinburgh, 1999.      *
*                         All rights reserved.                            *
*                                                                         *
**************************************************************************/

/*

Modified 3/3/97 by David M. Doolin (dmd) doolin@cs.utk.edu
Fixed error in matgen() method. Added some comments.

Modified 1/22/97 by Paul McMahan mcmahan@cs.utk.edu
Added more MacOS options to form.

Optimized by Jonathan Hardwick (jch@cs.cmu.edu), 3/28/96
Compare to Linkpack.java.
Optimizations performed:
 - added "final" modifier to performance-critical methods.
 - changed lines of the form "a[i] = a[i] + x" to "a[i] += x".
 - minimized array references using common subexpression elimination.
 - eliminated unused variables.
 - undid an unrolled loop.
 - added temporary 1D arrays to hold frequently-used columns of 2D arrays.
 - wrote my own abs() method
See http://www.cs.cmu.edu/~jch/java/linpack.html for more details.


Ported to Java by Reed Wade  (wade@cs.utk.edu) 2/96
built using JDK 1.0 on solaris
using "javac -O Linpack.java"


Translated to C by Bonnie Toy 5/88
  (modified on 2/25/94  to fix a problem with daxpy  for
   unequal increments or equal increments not equal to 1.
     Jack Dongarra)

*/

package jomp.lufact; 

import jomp.jgfutil.*; 


public class Linpack {

  double a[][];
  double a_one_dim[][];
  double b[];
  double x[];
  double ops,total,norma,normx;
  double resid,time;
  double kf;
  int n,i,ntimes,info,lda,ldaa,kflops;
  int ipvt[];
    
 /*@Perm(requires="pure(#0) in alive",
		  61 ensures="pure(#0) in alive")*/
   final double abs (double d) {
    return (d >= 0) ? d : -d;
  }

 /* Vertex Name = a, Post Permissions = share, Pre-Permissions =share
  * Vertex Name = lda, Post Permissions = pure, Pre-Permissions =pure
	Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
	Vertex Name = b, Post Permissions = share, Pre-Permissions =share*/
  
   /*@Perm(requires="full(#0) * pure(#1) * pure(#2) * full(#3) in alive",
		   52 ensures="full(#0) * pure(#1) * pure(#2) * full(#3) in alive")*/
  final double matgen (double a[][], int lda, int n, double b[])
  {
	System.out.println("n = "+n);
    double norma;
    int init, i, j; 
    
    init = 1325;
   
    norma = 0.0;
/*  Next two for() statements switched.  Solver wants
matrix in column order. --dmd 3/3/97
*/
      for (i = 0; i < n; i++) {
    for (j = 0; j < n; j++) {
	init = 3125*init % 65536;
	a[j][i] = (init - 32768.0)/16384.0;
	norma = (a[j][i] > norma) ? a[j][i] : norma;
      }
    }
    for (i = 0; i < n; i++) {
      b[i] = 0.0;
    }
    for (j = 0; j < n; j++) {
      for (i = 0; i < n; i++) {
	b[i] += a[j][i];
      }
    }
    
    return norma;
  }
  

  
  /*
    dgefa factors a double precision matrix by gaussian elimination.
    
    dgefa is usually called by dgeco, but it can be called
    directly with a saving in time if  rcond  is not needed.
    (time for dgeco) = (1 + 9/n)*(time for dgefa) .
    
    on entry
    
    a       double precision[n][lda]
    the matrix to be factored.
    
    lda     integer
    the leading dimension of the array  a .
    
    n       integer
    the order of the matrix  a .
    
    on return
    
    a       an upper triangular matrix and the multipliers
    which were used to obtain it.
    the factorization can be written  a = l*u  where
    l  is a product of permutation and unit lower
    triangular matrices and  u  is upper triangular.
    
    ipvt    integer[n]
    an integer vector of pivot indices.
    
    info    integer
    = 0  normal value.
    = k  if  u[k][k] .eq. 0.0 .  this is not an error
    condition for this subroutine, but it does
    indicate that dgesl or dgedi will divide by zero
    if called.  use  rcond  in dgeco for a reliable
    indication of singularity.
    
    linpack. this version dated 08/14/78.
    cleve moler, university of new mexico, argonne national lab.
    
    functions
    
    blas daxpy,dscal,idamax
  */
 /* Method Name = dgefa
		  Vertex Name = a, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = lda, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = ipvt, Post Permissions = share, Pre-Permissions =share*/
  
  /*@Perm(requires="full(this) * pure(#0) * pure(#1) * pure(#2) * full(#3) in alive",
		  70 ensures="full(this) * pure(#0) * pure(#1) * pure(#2) * full(#3) in alive")*/
  final int dgefa( double a[][], int lda, int n, int ipvt[])
  {
    double[] col_k, col_j;
    double t;
    int k,kp1,l,nm1;
    int info;
   
    // gaussian elimination with partial pivoting
    info = 0;
    nm1 = n - 1;
    if (nm1 >=  0) {
      for (k = 0; k < nm1; k++) {
     a_one_dim[k] = a[k];
	 col_k = a_one_dim[k];// alias made here pointing to global field
	 kp1 = k + 1;
	// find l = pivot inde
	
	//l = idamax(n-k,col_k,k,1) + k; // col_k = a = r
	
	l = idamax(n,col_k,k,1) + k; // col_k = a = r
	
	ipvt[k] = l;
	// zero pivot implies this column already triangularized
	if (col_k[l] != 0) {
	  // interchange if necessary
	  if (l != k) {
	    t = col_k[l];
	    col_k[l] = col_k[k];
	    col_k[k] = t;
	  }
	  // compute multipliers
	  
	  t = -1.0/col_k[k];
	  
	  dscal(n,t,col_k,kp1,1);  //col_k = a= rw
	 //dscal(n-(kp1),t,col_k,kp1,1);  //col_k = a= rw
	  
	  // row elimination with column indexing
	  //omp parallel for private(col_j, t)
	  for (int j = kp1; j < n; j++) {	//type added for JOMP
	    col_j = a[j];
	    t = col_j[l];
	    if (l != k) {
	      col_j[l] = col_j[k];
	      col_j[k] = t;// col_j = a = rw
	    }
	    daxpy(n,col_k,t,kp1,1,col_j,kp1,1);
	     //col_k = a = r, col_j = a = r
	    
	   //daxpy(n-(kp1),t,col_k,kp1,1,col_j,kp1,1); //col_k = a = r, col_j = a = r
	  }
	}
	else {
	  info = k;
	}
      }
    }
    ipvt[n-1] = n-1; // ipv = rw
    if (a[(n-1)][(n-1)] == 0) info = n-1;
    
    return info;
  }

  
  
  /*
    dgesl solves the double precision system
    a * x = b  or  trans(a) * x = b
    using the factors computed by dgeco or dgefa.
  
    on entry
  
    a       double precision[n][lda]
    the output from dgeco or dgefa.
  
    lda     integer
    the leading dimension of the array  a .
    
    n       integer
    the order of the matrix  a .
  
    ipvt    integer[n]
    the pivot vector from dgeco or dgefa.

    b       double precision[n]
    the right hand side vector.
    
    job     integer
    = 0         to solve  a*x = b ,
    = nonzero   to solve  trans(a)*x = b  where
    trans(a)  is the transpose.
    
    on return
    
    b       the solution vector  x .
    
    error condition
    
    a division by zero will occur if the input factor contains a
    zero on the diagonal.  technically this indicates singularity
    but it is often caused by improper arguments or improper
    setting of lda .  it will not occur if the subroutines are
    called correctly and if dgeco has set rcond .gt. 0.0
    or dgefa has set info .eq. 0 .
    
    to compute  inverse(a) * c  where  c  is a matrix
    with  p  columns
    dgeco(a,lda,n,ipvt,rcond,z)
    if (!rcond is too small){
    for (j=0,j<p,j++)
    dgesl(a,lda,n,ipvt,c[j][0],0);
    }
    
    linpack. this version dated 08/14/78 .
    cleve moler, university of new mexico, argonne national lab.
    
    functions
    
    blas daxpy,ddot
  */
  /*Method Name = dgesl
		  Vertex Name = a, Post Permissions = pure, Pre-Permissions =share
		  Vertex Name = lda, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = ipvt, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = b, Post Permissions = share, Pre-Permissions =share*/
  
 /* @Perm(requires="pure(#0) * pure(#1) * pure(#2) * pure(#3) * full(#4) in alive",
		  88 ensures="pure(#0) * pure(#1) * pure(#2) * pure(#3) * full(#4) in alive")*/
  final void dgesl( double a[][], int lda, int n, int ipvt[], double b[], int job)
  {
    double t;
    int k,kb,l,nm1,kp1;

    nm1 = n - 1;
    if (job == 0) {

      // job = 0 , solve  a * x = b.  first solve  l*y = b

   if (nm1 >= 1) {
	for (k = 0; k < nm1; k++) {
	  l = ipvt[k];
	  t = b[l];
	  if (l != k){
	    b[l] = b[k];
	    b[k] = t;
	  }
	  kp1 = k + 1;
	  //daxpy(n-(kp1),t,a[k],kp1,1,b,kp1,1); //a[] is passed here a = r, b = rw
	  daxpy(n,a[k],t,kp1,1,b,kp1,1); //a[] is passed here a = r, b = rw

	}
      }

      // now solve  u*x = y

      for (kb = 0; kb < n; kb++) {
	k = n - (kb + 1);
	b[k] /= a[k][k];//b[k] = b[k]/a[k][k]]
	t = -b[k];
	daxpy(k,a[k],t,0,1,b,0,1);//a = r and b = rw
      }
    }
    else {

      // job = nonzero, solve  trans(a) * x = b.  first solve  trans(u)*y = b

      for (k = 0; k < n; k++) {
    	  t = ddot(k,a[k],0,1,b,0,1);// a = r and b = r
    	  b[k] = (b[k] - t)/a[k][k];// b = rw
      }

      // now solve trans(l)*x = y 

      if (nm1 >= 1) {
	for (kb = 1; kb < nm1; kb++) {
	  k = n - (kb+1);
	  kp1 = k + 1;
	//  b[k] += ddot(n-(kp1),a[k],kp1,1,b,kp1,1);// a = r, b = r
	  b[k] += ddot(n,a[k],kp1,1,b,kp1,1);// a = r, b = r
		
	  l = ipvt[k];
	  if (l != k) {
	    t = b[l];
	    b[l] = b[k];
	    b[k] = t;
	  }
	}
      }
    }
  }



  /*
    constant times a vector plus a vector.
    jack dongarra, linpack, 3/11/78.
  */
  
  
   /*Method Name = daxpy
		  Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = a, Post Permissions = share, Pre-Permissions =share 
		  Vertex Name = b, Post Permissions = share, Pre-Permissions =share*/ 
  
  // for this i have to add Arrays of fields against every paramter having different fields mappings
  final void daxpy(int n, double dx[], double da, int dx_off, int incx,
	      double dy[], int dy_off, int incy)
  {
    int i,ix,iy;

    if ((n > 0) && (da != 0)) {
      if (incx != 1 || incy != 1) {

	// code for unequal increments or equal increments not equal to 1

	ix = 0;
	iy = 0;
	if (incx < 0) 
		ix = (-n+1)*incx;
	if (incy < 0) 
		iy = (-n+1)*incy;
	for (i = 0;i < n; i++) {
	  dy[iy +dy_off] = dy[iy +dy_off]+ da*dx[ix +dx_off];
	  ix += incx;
	  iy += incy;
	}
	return;
      } else {

	// code for both increments equal to 1

	for (i=0; i < n; i++)
	  dy[i +dy_off] = dy[i +dy_off] +da*dx[i +dx_off];
      }
    }
  }



  /*
    forms the dot product of two vectors.
    jack dongarra, linpack, 3/11/78.
  */
  //a = r and b = rw
  /*Method Name = ddot
		  Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = a, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = b, Post Permissions = pure, Pre-Permissions =pure*/ 
  final double ddot( int n, double dx[], int dx_off, int incx, double dy[],
	       int dy_off, int incy)
  {
    double dtemp;
    int i,ix,iy;

    dtemp = 0;

    if (n > 0) {
      
      if (incx != 1 || incy != 1) {

	// code for unequal increments or equal increments not equal to 1

	ix = 0;
	iy = 0;
	if (incx < 0) ix = (-n+1)*incx;
	if (incy < 0) iy = (-n+1)*incy;
	for (i = 0;i < n; i++) {
	  dtemp = dtemp+dx[ix +dx_off]*dy[iy +dy_off];
	  ix += incx;
	  iy += incy;
	}
      } else {

	// code for both increments equal to 1
	
	for (i=0;i < n; i++)
		//dtemp=0;
		dtemp = dtemp+dx[i +dx_off]*dy[i +dy_off];
	  //dtemp += dx[i +dx_off]*dy[i +dy_off];
		
      }
    }
    return(dtemp);
  }

  
  
  /*
    scales a vector by a constant.
    jack dongarra, linpack, 3/11/78.
  */
 /* Method Name = dscal
		  Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = a, Post Permissions = share, Pre-Permissions =share*/ 
  final void dscal(int n, double da, double dx[], int dx_off, int incx)
  {
    int i,nincx;

    if (n > 0) {
      if (incx != 1) {

	// code for increment not equal to 1

	nincx = n*incx;
	for (i = 0; i < nincx; i += incx)
	  dx[i +dx_off] *= da;
      } else {

	// code for increment equal to 1

	for (i = 0; i < n; i++)
	  dx[i +dx_off] *= da;
      }
    }
  }

  
  
  /*
    finds the index of element having max. absolute value.
    jack dongarra, linpack, 3/11/78.
  */
  /*Method Name = idamax
		  Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = a, Post Permissions = pure, Pre-Permissions =pure*/
  final int idamax( int n, double dx[], int dx_off, int incx)
  {
    double dmax, dtemp;
    int i, ix, itemp=0;

    if (n < 1) {
      itemp = -1;
    } else if (n ==1) {
      itemp = 0;
    } else if (incx != 1) {

      // code for increment not equal to 1

      dmax = abs(dx[0 +dx_off]);
      ix = 1 + incx;
      for (i = 1; i < n; i++) {
	dtemp = abs(dx[ix + dx_off]);
	if (dtemp > dmax)  {
	  itemp = i;
	  dmax = dtemp;
	}
	ix += incx;
      }
    } else {

      // code for increment equal to 1

      itemp = 0;
      dmax = abs(dx[0 +dx_off]);
      for (i = 1; i < n; i++) {
	dtemp = abs(dx[i + dx_off]);
	if (dtemp > dmax) {
	  itemp = i;
	  dmax = dtemp;
	}
      }
    }
    return (itemp);
  }


  
  /*
    estimate unit roundoff in quantities of size x.
    
    this program should function properly on all systems
    satisfying the following two assumptions,
    1.  the base used in representing dfloating point
    numbers is not a power of three.
    2.  the quantity  a  in statement 10 is represented to
    the accuracy used in dfloating point variables
    that are stored in memory.
    the statement number 10 and the go to 10 are intended to
    force optimizing compilers to generate code satisfying
    assumption 2.
    under these assumptions, it should be true that,
    a  is not exactly equal to four-thirds,
    b  has a zero for its last bit or digit,
    c  is not exactly equal to one,
    eps  measures the separation of 1.0 from
    the next larger dfloating point number.
    the developers of eispack would appreciate being informed
    about any systems where these assumptions do not hold.
    
    *****************************************************************
    this routine is one of the auxiliary routines used by eispack iii
    to avoid machine dependencies.
    *****************************************************************
  
    this version dated 4/6/83.
  */
  final double epslon (double x)
  {
    double a,b,c,eps;

    a = 4.0e0/3.0e0;
    eps = 0;
    while (eps == 0) {
      b = a - 1.0;
      c = b + b + b;
      eps = abs(c-1.0);
    }
    return(eps*abs(x));
  }

  

  /*
    purpose:
    multiply matrix m times vector x and add the result to vector y.
    
    parameters:
    
    n1 integer, number of elements in vector y, and number of rows in
    matrix m
    
    y double [n1], vector of length n1 to which is added
    the product m*x
    
    n2 integer, number of elements in vector x, and number of columns
    in matrix m
    
    ldm integer, leading dimension of array m
    
    x double [n2], vector of length n2
    
    m double [ldm][n2], matrix of n1 rows and n2 columns
  */
  
/* Method Name = dmxpy
		  Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = b, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = lda, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = x, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = a, Post Permissions = pure, Pre-Permissions =pure*/
  final void dmxpy (int n1, double y[], int n2, double x[], double m[][])
  {
    int j,i;

    // cleanup odd vector
    for (j = 0; j < n2; j++) {
      for (i = 0; i < n1; i++) {
	y[i] = y[i]+x[j]*m[j][i];
      }
    }
  }

}
/*Class Name = JGFLUFactBenchSizeB
Class Name = JGFLUFactBenchSizeB
Method Name = main
Vertex Name = ops, Post Permissions = unique, Pre-Permissions =none
Vertex Name = timers, Post Permissions = unique, Pre-Permissions =none
Vertex Name = name, Post Permissions = unique, Pre-Permissions =none
Vertex Name = opname, Post Permissions = unique, Pre-Permissions =none
Vertex Name = size, Post Permissions = unique, Pre-Permissions =none
Vertex Name = time, Post Permissions = unique, Pre-Permissions =none
Vertex Name = calls, Post Permissions = unique, Pre-Permissions =none
Vertex Name = opcount, Post Permissions = unique, Pre-Permissions =none
Vertex Name = on, Post Permissions = unique, Pre-Permissions =none
Vertex Name = n, Post Permissions = unique, Pre-Permissions =none
Vertex Name = datasizes, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ldaa, Post Permissions = unique, Pre-Permissions =none
Vertex Name = lda, Post Permissions = unique, Pre-Permissions =none
Vertex Name = a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = x, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ipvt, Post Permissions = unique, Pre-Permissions =none
Vertex Name = norma, Post Permissions = unique, Pre-Permissions =none
Vertex Name = info, Post Permissions = unique, Pre-Permissions =none
Vertex Name = start_time, Post Permissions = unique, Pre-Permissions =none
Vertex Name = resid, Post Permissions = unique, Pre-Permissions =none
Vertex Name = normx, Post Permissions = unique, Pre-Permissions =none
Method Name = printHeader
Method Name = JGFrun
Vertex Name = ops, Post Permissions = share, Pre-Permissions =share
Vertex Name = timers, Post Permissions = share, Pre-Permissions =share
Vertex Name = name, Post Permissions = share, Pre-Permissions =share
Vertex Name = opname, Post Permissions = share, Pre-Permissions =share
Vertex Name = size, Post Permissions = share, Pre-Permissions =share
Vertex Name = time, Post Permissions = share, Pre-Permissions =share
Vertex Name = calls, Post Permissions = share, Pre-Permissions =share
Vertex Name = opcount, Post Permissions = share, Pre-Permissions =share
Vertex Name = on, Post Permissions = share, Pre-Permissions =share
Vertex Name = n, Post Permissions = share, Pre-Permissions =share
Vertex Name = datasizes, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = ldaa, Post Permissions = share, Pre-Permissions =share
Vertex Name = lda, Post Permissions = share, Pre-Permissions =share
Vertex Name = a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = x, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ipvt, Post Permissions = unique, Pre-Permissions =none
Vertex Name = norma, Post Permissions = share, Pre-Permissions =share
Vertex Name = info, Post Permissions = share, Pre-Permissions =share
Vertex Name = start_time, Post Permissions = share, Pre-Permissions =share
Vertex Name = resid, Post Permissions = share, Pre-Permissions =share
Vertex Name = normx, Post Permissions = share, Pre-Permissions =share
Method Name = addTimer
Vertex Name = timers, Post Permissions = share, Pre-Permissions =share
Vertex Name = name, Post Permissions = share, Pre-Permissions =share
Vertex Name = opname, Post Permissions = share, Pre-Permissions =share
Vertex Name = size, Post Permissions = share, Pre-Permissions =share
Vertex Name = time, Post Permissions = share, Pre-Permissions =share
Vertex Name = calls, Post Permissions = share, Pre-Permissions =share
Vertex Name = opcount, Post Permissions = share, Pre-Permissions =share
Vertex Name = on, Post Permissions = share, Pre-Permissions =share
Method Name = JGFTimer
Vertex Name = name, Post Permissions = share, Pre-Permissions =share
Vertex Name = opname, Post Permissions = share, Pre-Permissions =share
Vertex Name = size, Post Permissions = share, Pre-Permissions =share
Vertex Name = time, Post Permissions = share, Pre-Permissions =share
Vertex Name = calls, Post Permissions = share, Pre-Permissions =share
Vertex Name = opcount, Post Permissions = share, Pre-Permissions =share
Vertex Name = on, Post Permissions = share, Pre-Permissions =share
Method Name = reset
Vertex Name = time, Post Permissions = share, Pre-Permissions =share
Vertex Name = calls, Post Permissions = share, Pre-Permissions =share
Vertex Name = opcount, Post Permissions = share, Pre-Permissions =share
Vertex Name = on, Post Permissions = share, Pre-Permissions =share
Method Name = JGFsetsize
Vertex Name = size, Post Permissions = share, Pre-Permissions =share
Method Name = JGFinitialise
Vertex Name = n, Post Permissions = share, Pre-Permissions =share
Vertex Name = datasizes, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = size, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ldaa, Post Permissions = share, Pre-Permissions =share
Vertex Name = lda, Post Permissions = share, Pre-Permissions =share
Vertex Name = a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = x, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ipvt, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ops, Post Permissions = share, Pre-Permissions =share
Vertex Name = norma, Post Permissions = share, Pre-Permissions =share
Method Name = matgen
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = lda, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Method Name = JGFkernel
Vertex Name = info, Post Permissions = share, Pre-Permissions =share
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = lda, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ipvt, Post Permissions = share, Pre-Permissions =share
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = timers, Post Permissions = share, Pre-Permissions =share
Vertex Name = on, Post Permissions = share, Pre-Permissions =share
Vertex Name = name, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = start_time, Post Permissions = share, Pre-Permissions =share
Vertex Name = time, Post Permissions = share, Pre-Permissions =share
Vertex Name = calls, Post Permissions = share, Pre-Permissions =share
Method Name = startTimer
Vertex Name = timers, Post Permissions = share, Pre-Permissions =share
Vertex Name = on, Post Permissions = share, Pre-Permissions =share
Vertex Name = name, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = start_time, Post Permissions = share, Pre-Permissions =share
Method Name = start
Vertex Name = on, Post Permissions = share, Pre-Permissions =share
Vertex Name = name, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = start_time, Post Permissions = share, Pre-Permissions =share
Method Name = dgefa
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = lda, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ipvt, Post Permissions = share, Pre-Permissions =share
Method Name = idamax
Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = a, Post Permissions = pure, Pre-Permissions =pure
Method Name = abs
Method Name = dscal
Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Method Name = daxpy
Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Method Name = dgesl
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = lda, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ipvt, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Method Name = ddot
Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = a, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = b, Post Permissions = pure, Pre-Permissions =pure
Method Name = stopTimer
Vertex Name = timers, Post Permissions = share, Pre-Permissions =share
Vertex Name = time, Post Permissions = share, Pre-Permissions =share
Vertex Name = start_time, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = on, Post Permissions = share, Pre-Permissions =share
Vertex Name = name, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = calls, Post Permissions = share, Pre-Permissions =share
Method Name = stop
Vertex Name = time, Post Permissions = share, Pre-Permissions =share
Vertex Name = start_time, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = on, Post Permissions = share, Pre-Permissions =share
Vertex Name = name, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = calls, Post Permissions = share, Pre-Permissions =share
Method Name = JGFvalidate
Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = x, Post Permissions = share, Pre-Permissions =share
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = norma, Post Permissions = share, Pre-Permissions =share
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = lda, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = resid, Post Permissions = share, Pre-Permissions =share
Vertex Name = normx, Post Permissions = share, Pre-Permissions =share
Vertex Name = size, Post Permissions = pure, Pre-Permissions =pure
Method Name = dmxpy
Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = lda, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = x, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = a, Post Permissions = pure, Pre-Permissions =pure
Method Name = epslon
Method Name = JGFtidyup
Vertex Name = a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = x, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ipvt, Post Permissions = unique, Pre-Permissions =none
Method Name = addOpsToTimer
Vertex Name = timers, Post Permissions = share, Pre-Permissions =share
Vertex Name = opcount, Post Permissions = share, Pre-Permissions =share
Method Name = addops
Vertex Name = opcount, Post Permissions = share, Pre-Permissions =share
Method Name = printTimer
Vertex Name = timers, Post Permissions = share, Pre-Permissions =share
Vertex Name = opname, Post Permissions = share, Pre-Permissions =share
Vertex Name = name, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = time, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = size, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = opcount, Post Permissions = pure, Pre-Permissions =pure
Method Name = print
Vertex Name = opname, Post Permissions = share, Pre-Permissions =share
Vertex Name = name, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = time, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = size, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = opcount, Post Permissions = pure, Pre-Permissions =pure
Method Name = perf
Vertex Name = opcount, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = time, Post Permissions = pure, Pre-Permissions =pure
Class Name = JGFLUFactBench
Class Name = JGFLUFactBenchSizeB
Method Name = main
Vertex Name = ops, Post Permissions = unique, Pre-Permissions =none
Vertex Name = timers, Post Permissions = unique, Pre-Permissions =none
Vertex Name = name, Post Permissions = unique, Pre-Permissions =none
Vertex Name = opname, Post Permissions = unique, Pre-Permissions =none
Vertex Name = size, Post Permissions = unique, Pre-Permissions =none
Vertex Name = time, Post Permissions = unique, Pre-Permissions =none
Vertex Name = calls, Post Permissions = unique, Pre-Permissions =none
Vertex Name = opcount, Post Permissions = unique, Pre-Permissions =none
Vertex Name = on, Post Permissions = unique, Pre-Permissions =none
Vertex Name = n, Post Permissions = unique, Pre-Permissions =none
Vertex Name = datasizes, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ldaa, Post Permissions = unique, Pre-Permissions =none
Vertex Name = lda, Post Permissions = unique, Pre-Permissions =none
Vertex Name = a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = x, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ipvt, Post Permissions = unique, Pre-Permissions =none
Vertex Name = norma, Post Permissions = unique, Pre-Permissions =none
Vertex Name = info, Post Permissions = unique, Pre-Permissions =none
Vertex Name = start_time, Post Permissions = unique, Pre-Permissions =none
Vertex Name = resid, Post Permissions = unique, Pre-Permissions =none
Vertex Name = normx, Post Permissions = unique, Pre-Permissions =none
Method Name = JGFsetsize
Vertex Name = size, Post Permissions = share, Pre-Permissions =share
Method Name = JGFinitialise
Vertex Name = n, Post Permissions = share, Pre-Permissions =share
Vertex Name = datasizes, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = size, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ldaa, Post Permissions = share, Pre-Permissions =share
Vertex Name = lda, Post Permissions = share, Pre-Permissions =share
Vertex Name = a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = x, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ipvt, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ops, Post Permissions = share, Pre-Permissions =share
Vertex Name = norma, Post Permissions = share, Pre-Permissions =share
Method Name = JGFkernel
Vertex Name = info, Post Permissions = share, Pre-Permissions =share
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = lda, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ipvt, Post Permissions = share, Pre-Permissions =share
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = timers, Post Permissions = share, Pre-Permissions =share
Vertex Name = on, Post Permissions = share, Pre-Permissions =share
Vertex Name = name, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = start_time, Post Permissions = share, Pre-Permissions =share
Vertex Name = time, Post Permissions = share, Pre-Permissions =share
Vertex Name = calls, Post Permissions = share, Pre-Permissions =share
Method Name = JGFvalidate
Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = x, Post Permissions = share, Pre-Permissions =share
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = norma, Post Permissions = share, Pre-Permissions =share
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = lda, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = resid, Post Permissions = share, Pre-Permissions =share
Vertex Name = normx, Post Permissions = share, Pre-Permissions =share
Vertex Name = size, Post Permissions = pure, Pre-Permissions =pure
Method Name = JGFtidyup
Vertex Name = a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = x, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ipvt, Post Permissions = unique, Pre-Permissions =none
Method Name = JGFrun
Vertex Name = ops, Post Permissions = share, Pre-Permissions =share
Vertex Name = timers, Post Permissions = share, Pre-Permissions =share
Vertex Name = name, Post Permissions = share, Pre-Permissions =share
Vertex Name = opname, Post Permissions = share, Pre-Permissions =share
Vertex Name = size, Post Permissions = share, Pre-Permissions =share
Vertex Name = time, Post Permissions = share, Pre-Permissions =share
Vertex Name = calls, Post Permissions = share, Pre-Permissions =share
Vertex Name = opcount, Post Permissions = share, Pre-Permissions =share
Vertex Name = on, Post Permissions = share, Pre-Permissions =share
Vertex Name = n, Post Permissions = share, Pre-Permissions =share
Vertex Name = datasizes, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = ldaa, Post Permissions = share, Pre-Permissions =share
Vertex Name = lda, Post Permissions = share, Pre-Permissions =share
Vertex Name = a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = x, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ipvt, Post Permissions = unique, Pre-Permissions =none
Vertex Name = norma, Post Permissions = share, Pre-Permissions =share
Vertex Name = info, Post Permissions = share, Pre-Permissions =share
Vertex Name = start_time, Post Permissions = share, Pre-Permissions =share
Vertex Name = resid, Post Permissions = share, Pre-Permissions =share
Vertex Name = normx, Post Permissions = share, Pre-Permissions =share
Class Name = Linpack
Class Name = JGFLUFactBenchSizeB
Method Name = main
Vertex Name = ops, Post Permissions = unique, Pre-Permissions =none
Vertex Name = timers, Post Permissions = unique, Pre-Permissions =none
Vertex Name = name, Post Permissions = unique, Pre-Permissions =none
Vertex Name = opname, Post Permissions = unique, Pre-Permissions =none
Vertex Name = size, Post Permissions = unique, Pre-Permissions =none
Vertex Name = time, Post Permissions = unique, Pre-Permissions =none
Vertex Name = calls, Post Permissions = unique, Pre-Permissions =none
Vertex Name = opcount, Post Permissions = unique, Pre-Permissions =none
Vertex Name = on, Post Permissions = unique, Pre-Permissions =none
Vertex Name = n, Post Permissions = unique, Pre-Permissions =none
Vertex Name = datasizes, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ldaa, Post Permissions = unique, Pre-Permissions =none
Vertex Name = lda, Post Permissions = unique, Pre-Permissions =none
Vertex Name = a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = x, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ipvt, Post Permissions = unique, Pre-Permissions =none
Vertex Name = norma, Post Permissions = unique, Pre-Permissions =none
Vertex Name = info, Post Permissions = unique, Pre-Permissions =none
Vertex Name = start_time, Post Permissions = unique, Pre-Permissions =none
Vertex Name = resid, Post Permissions = unique, Pre-Permissions =none
Vertex Name = normx, Post Permissions = unique, Pre-Permissions =none
Method Name = abs
Method Name = matgen
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = lda, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Method Name = dgefa
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = lda, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ipvt, Post Permissions = share, Pre-Permissions =share
Method Name = dgesl
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = lda, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ipvt, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Method Name = daxpy
Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Method Name = ddot
Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = a, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = b, Post Permissions = pure, Pre-Permissions =pure
Method Name = dscal
Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Method Name = idamax
Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = a, Post Permissions = pure, Pre-Permissions =pure
Method Name = epslon
Method Name = dmxpy
Vertex Name = n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = lda, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = x, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = a, Post Permissions = pure, Pre-Permissions =pure
*/
//////////////////////////////////////////////////////////////////////
/*Method Name = abs
Method Name = matgen
Vertex Name = lub.a, Post Permissions = share, Pre-Permissions =share
Vertex Name = lub.lda, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = lub.n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = lub.b, Post Permissions = share, Pre-Permissions =share
Method Name = dgefa
Vertex Name = lub.a, Post Permissions = share, Pre-Permissions =share
Vertex Name = lub.lda, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = lub.n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = lub.ipvt, Post Permissions = share, Pre-Permissions =share
Method Name = dgesl
Vertex Name = lub.a, Post Permissions = share, Pre-Permissions =share
Vertex Name = lub.lda, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = lub.n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = lub.ipvt, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = lub.b, Post Permissions = share, Pre-Permissions =share
Method Name = daxpy
Vertex Name = lub.n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = lub.a, Post Permissions = share, Pre-Permissions =share
Vertex Name = lub.b, Post Permissions = share, Pre-Permissions =share
Method Name = ddot
Vertex Name = lub.n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = lub.a, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = lub.b, Post Permissions = pure, Pre-Permissions =pure
Method Name = dscal
Vertex Name = lub.n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = lub.a, Post Permissions = share, Pre-Permissions =share
Method Name = idamax
Vertex Name = lub.n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = lub.a, Post Permissions = pure, Pre-Permissions =pure
Method Name = epslon
Method Name = dmxpy
Vertex Name = lub.n, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = lub.b, Post Permissions = share, Pre-Permissions =share
Vertex Name = lub.lda, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = lub.x, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = lub.a, Post Permissions = pure, Pre-Permissions =pure/

Class Name = JGFLUFactBenchSizeB
Method Name = main
Ref-Var= lub.size, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.n, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.datasizes, Pre-Permissions=none, Post Permissions=none
Ref-Var= lub.ldaa, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.lda, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.a, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.b, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.x, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.ipvt, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.ops, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.norma, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.info, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.resid, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.normx, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lufact.Linpack.datasizes, Pre-Permissions=none, Post Permissions=unique
Method Name = printHeader
Method Name = JGFrun
Ref-Var= lub.size, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.n, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.datasizes, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= lub.ldaa, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.lda, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.a, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.b, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.x, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.ipvt, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.ops, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.norma, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.info, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.resid, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.normx, Pre-Permissions=share, Post Permissions=share
Method Name = JGFsetsize
Ref-Var= lub.size, Pre-Permissions=share, Post Permissions=share
Method Name = JGFinitialise
Ref-Var= lub.n, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.datasizes, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= lub.size, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.ldaa, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.lda, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.a, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.b, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.x, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.ipvt, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.ops, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.norma, Pre-Permissions=share, Post Permissions=share
Method Name = matgen
Ref-Var= lub.a, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.lda, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.n, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.b, Pre-Permissions=share, Post Permissions=share
Method Name = JGFkernel
Ref-Var= lub.info, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.a, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.lda, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.n, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.ipvt, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.b, Pre-Permissions=share, Post Permissions=share
Method Name = dgefa
Ref-Var= lub.a, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.lda, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.n, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.ipvt, Pre-Permissions=share, Post Permissions=share
Method Name = idamax
Ref-Var= lub.n, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.a, Pre-Permissions=pure, Post Permissions=pure
Method Name = abs
Method Name = dscal
Ref-Var= lub.n, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.a, Pre-Permissions=share, Post Permissions=share
Method Name = daxpy
Ref-Var= lub.n, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.a, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.b, Pre-Permissions=share, Post Permissions=share
Method Name = dgesl
Ref-Var= lub.a, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.lda, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.n, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.ipvt, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.b, Pre-Permissions=share, Post Permissions=share
Method Name = ddot
Ref-Var= lub.n, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.a, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.b, Pre-Permissions=pure, Post Permissions=pure
Method Name = JGFvalidate
Ref-Var= lub.n, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.x, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.b, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.norma, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.a, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.lda, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.resid, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.normx, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.size, Pre-Permissions=pure, Post Permissions=pure
Method Name = dmxpy
Ref-Var= lub.n, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.b, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.lda, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.x, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.a, Pre-Permissions=pure, Post Permissions=pure
Method Name = epslon
Method Name = JGFtidyup
Ref-Var= lub.a, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.b, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.x, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.ipvt, Pre-Permissions=none, Post Permissions=unique

//////////////////////////////////////////////////////


//////////////////////////////////////////////////////

//////////////////////////////////////////////////////
Class Name = JGFLUFactBench
Method Name = JGFsetsize
Ref-Var= lub.size, Pre-Permissions=share, Post Permissions=share
Method Name = JGFinitialise
Ref-Var= lub.n, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.datasizes, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= lub.size, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.ldaa, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.lda, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.a, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.b, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.x, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.ipvt, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.ops, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.norma, Pre-Permissions=share, Post Permissions=share
Method Name = JGFkernel
Ref-Var= lub.info, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.a, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.lda, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.n, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.ipvt, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.b, Pre-Permissions=share, Post Permissions=share
Method Name = JGFvalidate
Ref-Var= lub.n, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.x, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.b, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.norma, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.a, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.lda, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.resid, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.normx, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.size, Pre-Permissions=pure, Post Permissions=pure
Method Name = JGFtidyup
Ref-Var= lub.a, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.b, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.x, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.ipvt, Pre-Permissions=none, Post Permissions=unique
Method Name = JGFrun
Ref-Var= lub.size, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.n, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.datasizes, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= lub.ldaa, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.lda, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.a, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.b, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.x, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.ipvt, Pre-Permissions=none, Post Permissions=unique
Ref-Var= lub.ops, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.norma, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.info, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.resid, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.normx, Pre-Permissions=share, Post Permissions=share

//////////////////////////////////////////////////////


//////////////////////////////////////////////////////

//////////////////////////////////////////////////////
Class Name = Linpack
Method Name = abs
Method Name = matgen
Ref-Var= lub.a, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.lda, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.n, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.b, Pre-Permissions=share, Post Permissions=share
Method Name = dgefa
Ref-Var= lub.a, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.lda, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.n, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.ipvt, Pre-Permissions=share, Post Permissions=share
Method Name = dgesl
Ref-Var= lub.a, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.lda, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.n, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.ipvt, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.b, Pre-Permissions=share, Post Permissions=share
Method Name = daxpy
Ref-Var= lub.n, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.a, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.b, Pre-Permissions=share, Post Permissions=share
Method Name = ddot
Ref-Var= lub.n, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.a, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.b, Pre-Permissions=pure, Post Permissions=pure
Method Name = dscal
Ref-Var= lub.n, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.a, Pre-Permissions=share, Post Permissions=share
Method Name = idamax
Ref-Var= lub.n, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.a, Pre-Permissions=pure, Post Permissions=pure
Method Name = epslon
Method Name = dmxpy
Ref-Var= lub.n, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.b, Pre-Permissions=share, Post Permissions=share
Ref-Var= lub.lda, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.x, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= lub.a, Pre-Permissions=pure, Post Permissions=pure*/