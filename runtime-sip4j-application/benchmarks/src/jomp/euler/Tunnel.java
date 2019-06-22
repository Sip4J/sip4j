
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
*                  Original version of this code by                       *
*                     David Oh (bamf@pobox.com)                           *
*                                                                         *
*      This version copyright (c) The University of Edinburgh, 1999.      *
*                         All rights reserved.                            *
*                                                                         *
**************************************************************************/


package jomp.euler; 

import java.io.*; 

import edu.cmu.cs.plural.annot.Perm;

import jomp.jgfutil.JGFInstrumentor;

public class Tunnel{

  int size;
  int datasizes[] = {8,12};

  double machff = 0.7;    /* Inflow mach number */
  public double secondOrderDamping = 1.0;
  public double fourthOrderDamping = 1.0;
  public int ntime = 1; /* 0 = local timestep, 1 = time accurate */ 
  int scale; /* Refine input grid by this factor */ 
  double error;

  double aofTunnel[][];   /* Grid cell area */
  double  deltat[][];   /* Timestep */
  double opg[][], pg[][], pg1[][];                     /* Pressure */
  double sxi[][], seta[][];
  double tg[][], tg1[][];                           /* Temperature */
  double xnode[][], ynode[][];      /* Storage of node coordinates */

  double oldval[][][], newval[][][]; /* Tepmoray arrays for interpolation */ 

  double cff, uff, vff, pff, rhoff, tff, jplusff, jminusff;
                                    /* Far field values */
  double datamax, datamin;
  int iter = 100; /* Number of iterations */ 
  int imax, jmax;     /* Number of nodes in x and y direction*/
  int imaxin, jmaxin; /* Number of nodes in x and y direction in unscaled data */ 
  int nf = 6; /* Number of fields in data file */ 
  Statevector d[][];   /* Damping coefficients */
  Statevector f[][], g[][];   /* Flux Vectors */
  Statevector r[][], ug1[][];
  Statevector ug[][];      /* Storage of data */
  final double Cp = 1004.5;      /* specific heat, const pres. */
  final double Cv=717.5;      /* specific heat, const vol. */
  final double gamma=1.4;   /* Ratio of specific heats */
  final double rgas=287.0;       /* Gas Constant */
  final double fourthOrderNormalizer = 0.02; /* Damping coefficients */
  final double secondOrderNormalizer = 0.02;


 /* Method Name = initialise
		  Vertex Name = scale, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = datasizes, Post Permissions = immutable, Pre-Permissions =immutable
		  Vertex Name = size, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = imaxin, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = jmaxin, Post Permissions = share, Pre-Permissions =share
		   Vertex Name = nf, Post Permissions = immutable, Pre-Permissions =immutable
		  Vertex Name = imax, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = jmax, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = oldval, Post Permissions = unique, Pre-Permissions =none
		  Vertex Name = newval, Post Permissions = unique, Pre-Permissions =none
		  Vertex Name = deltat, Post Permissions = unique, Pre-Permissions =none
		  Vertex Name = opg, Post Permissions = unique, Pre-Permissions =none
		  Vertex Name = pg, Post Permissions = unique, Pre-Permissions =none
		  Vertex Name = pg1, Post Permissions = unique, Pre-Permissions =none
		  Vertex Name = sxi, Post Permissions = unique, Pre-Permissions =none
		  Vertex Name = seta, Post Permissions = unique, Pre-Permissions =none
		  Vertex Name = g, Post Permissions = unique, Pre-Permissions =none
		  Vertex Name = r, Post Permissions = unique, Pre-Permissions =none
		  Vertex Name = tg, Post Permissions = unique, Pre-Permissions =none
		  Vertex Name = tg1, Post Permissions = unique, Pre-Permissions =none
		  Vertex Name = ug, Post Permissions = unique, Pre-Permissions =none
		  Vertex Name = f, Post Permissions = unique, Pre-Permissions =none
		  Vertex Name = ug1, Post Permissions = unique, Pre-Permissions =none
		  Vertex Name = xnode, Post Permissions = unique, Pre-Permissions =none
		  Vertex Name = ynode, Post Permissions = unique, Pre-Permissions =none
		  Vertex Name = cff, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = vff, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = pff, Post Permissions = share, Pre-Permissions =share
		   Vertex Name = tff, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = rhoff, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = a, Post Permissions = unique, Pre-Permissions =share // associate object info
		  Vertex Name = d, Post Permissions = unique, Pre-Permissions =share // associate object info
		  Vertex Name = b, Post Permissions = unique, Pre-Permissions =share // associate object info
		  Vertex Name = c, Post Permissions = unique, Pre-Permissions =share // associate object info
		  Vertex Name = gamma, Post Permissions = immutable, Pre-Permissions =immutable
		  Vertex Name = rgas, Post Permissions = immutable, Pre-Permissions =immutable
		  Vertex Name = Cv, Post Permissions = immutable, Pre-Permissions =immutable*/
  public void initialise() throws Exception{

    int i, j, k, n;             /* Dummy counters */
    double scrap, scrap2;     /* Temporary storage */


    /* Set scale factor for interpolation */ 
    scale = datasizes[size]; // scale = rw
 
    /* Open data file */
    FileReader instream = new FileReader("tunnel.dat");

    /* Convert the stream into tokens (which helps you parse it) */
    StreamTokenizer intokens = new StreamTokenizer(instream);
    
    /* Read header */
    if (intokens.nextToken() == StreamTokenizer.TT_NUMBER) //TT_NUMBER = r
      imaxin = (int) intokens.nval;//imaxin = rw, nval = r
    else
      throw new IOException();
    if (intokens.nextToken() == StreamTokenizer.TT_NUMBER)
      jmaxin = (int) intokens.nval;//jmaxin = rw, nval = r
    else
      throw new IOException();

    // Read data into temporary array 
    // note: dummy extra row and column needed to make interpolation simple
    oldval = new double[nf][imaxin+1][jmaxin+1]; //oldval = rw, null, nf = r, 

    for (i=0;i<imaxin;i++){
      for (j=0;j<jmaxin;j++){
	for (k=0; k<nf; k++){
	  if (intokens.nextToken() == StreamTokenizer.TT_NUMBER){
	    oldval[k][i][j]=  (double) intokens.nval;
	  }
	  else{
	    throw new IOException(); 
	  }
	}
      }
    }

    //interpolate onto finer grid 

    imax = (imaxin - 1) * scale + 1; // scale = r
    jmax = (jmaxin - 1) * scale + 1; // 

    newval = new double[nf][imax][jmax];//newval = rw,null
    
    for (k=0; k<nf; k++){
      for (i=0;i<imax;i++){
	for (j=0;j<jmax;j++){
	  int iold = i/scale; 
          int jold = j/scale; 
          double xf = ( (double) i%scale) /( (double) scale); 
          double yf = ( (double) j%scale) /( (double) scale); 
          	newval[k][i][j] = (1.0 - xf)*(1.0 - yf)* oldval[k][iold][jold]
	                +(1.0 - xf)*       yf * oldval[k][iold][jold+1]
                        +       xf *(1.0 - yf)* oldval[k][iold+1][jold]
                        +       xf *       yf * oldval[k][iold+1][jold+1];
	}
      }
    }

    
    //create arrays 

    //all object creations rw, null
    deltat = new double[imax+1][jmax+2];
    opg = new double[imax+2][jmax+2];
    pg = new double[imax+2][jmax+2];
    pg1 = new double[imax+2][jmax+2];
    sxi =new double[imax+2][jmax+2];;
    seta = new double[imax+2][jmax+2];;
    tg = new double[imax+2][jmax+2];
    tg1 = new double[imax+2][jmax+2];
    ug = new Statevector[imax+2][jmax+2];
    aofTunnel = new double[imax][jmax]; // object = eb
    d =  new Statevector[imax+2][jmax+2];
    f =  new Statevector[imax+2][jmax+2];
    g =  new Statevector[imax+2][jmax+2];
    r =  new Statevector[imax+2][jmax+2];
    ug1 =  new Statevector[imax+2][jmax+2];
    xnode = new double[imax][jmax];
    ynode = new double[imax][jmax];

    for (i = 0; i < imax+2; i++)
      for (j = 0; j < jmax+2; ++j) {
   
    d[i][j] =  new Statevector();
	f[i][j] =  new Statevector();
	g[i][j] =  new Statevector();
	r[i][j] = new Statevector();
	ug[i][j] = new Statevector();
	ug1[i][j] = new Statevector();
      }

    /* Set farfield values (we use normalized units for everything */    
    cff = 1.0; //cff = rw
    vff = 0.0;// vff = rw
    pff = 1.0 / gamma;// pff = rw amd gamma = r
    rhoff = 1.0;// rhoff = rw
    tff = pff / (rhoff * rgas);// tff = rw and rgas = r

    // Copy the interpolated data to arrays 


    for (i=0; i<imax; i++){
      for (j=0; j<jmax; j++){

	 xnode[i][j] = newval[0][i][j];// xnode = rw
	 ynode[i][j] = newval[1][i][j];// ynode = rw
        ug[i+1][j+1].a = newval[2][i][j];// a = rw,null object = eb.ug
        ug[i+1][j+1].b = newval[3][i][j];// b= rw,null object eb.ug
        ug[i+1][j+1].c = newval[4][i][j];// c = rw,null objecct eb.ug
        ug[i+1][j+1].d = newval[5][i][j];// d = rw,null object eb.ug

	scrap = ug[i+1][j+1].c/ug[i+1][j+1].a;
	scrap2 = ug[i+1][j+1].b/ug[i+1][j+1].a;
	tg[i+1][j+1] = ug[i+1][j+1].d/ug[i+1][j+1].a 
	  - (0.5 * (scrap*scrap + scrap2*scrap2));
	tg[i+1][j+1] = tg[i+1][j+1] / Cv;
	pg[i+1][j+1] = rgas * ug[i+1][j+1].a * tg[i+1][j+1];

      }
    }
     
	
    /* Calculate grid cell areas */
    for (i = 1; i < imax; i++)
      for (j = 1; j < jmax; ++j)
    	  aofTunnel[i][j] = 0.5 * ((xnode[i][j] - xnode[i-1][j-1])   // a = rw, object = eb
			              * (ynode[i-1][j] - ynode[i][j-1])-
			 (ynode[i][j] - ynode[i-1][j-1]) 
			              * (xnode[i-1][j] - xnode[i][j-1]));
    // throw away temporary arrays 
    	oldval = null; 
    	newval = null;// null adress flow
  }
 /* Method Name = doIteration
		  Vertex Name = imax, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = jmax, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = opg, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = pg, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = ug, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = a, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = ug1, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = deltat, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = r, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = d, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = b, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = c, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = pg1, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = tg1, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = error, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = uff, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = machff, Post Permissions = immutable, Pre-Permissions =immutable
		  Vertex Name = jplusff, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = gamma, Post Permissions = immutable, Pre-Permissions =immutable
		  Vertex Name = cff, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = jminusff, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = ihat, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = xnode, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = jhat, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = ynode, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = Cv, Post Permissions = immutable, Pre-Permissions =immutable
		  Vertex Name = rgas, Post Permissions = immutable, Pre-Permissions =immutable
		  Vertex Name = rhoff, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = vff, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = tff, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = pff, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = ntime, Post Permissions = immutable, Pre-Permissions =immutable
		  Vertex Name = secondOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
		  Vertex Name = secondOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
		  Vertex Name = fourthOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
		  Vertex Name = fourthOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
		  Vertex Name = sxi, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = seta, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = f, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = Cp, Post Permissions = immutable, Pre-Permissions =immutable
		  Vertex Name = g, Post Permissions = pure, Pre-Permissions =pure*/

  void doIteration() {
    double scrap;
    int i,j; 
    Vector2 crap;

    /* Record the old pressure values */
    //omp for 
    for (i = 1; i < imax; ++i)
      for (j = 1; j < jmax; ++j) {
    	  opg[i][j] = pg[i][j];
      }

    calculateDummyCells(pg, tg, ug);//full(this) * full(pg), full(tg), full(ug)
    calculateDeltaT(); // full(this)
    calculateDamping(pg, ug);// full(this) * pure(0) * pure(1)

     //Do the integration 
     //Step 1 
    calculateF(pg, tg, ug);//full(this)* pure(params)
    calculateG(pg, tg, ug);//full(this)* pure(params)
    calculateR();//full(this)
    
    //omp for 
    for (i = 1; i < imax; i++){
      for (j = 1; j < jmax; ++j) {
		ug1[i][j].a=ug[i][j].a-0.25*deltat[i][j]/aofTunnel[i][j]*(r[i][j].a-d[i][j].a);
		ug1[i][j].b=ug[i][j].b-0.25*deltat[i][j]/aofTunnel[i][j]*(r[i][j].b-d[i][j].b);
		ug1[i][j].c=ug[i][j].c-0.25*deltat[i][j]/aofTunnel[i][j]*(r[i][j].c-d[i][j].c);
		ug1[i][j].d=ug[i][j].d-0.25*deltat[i][j]/aofTunnel[i][j]*(r[i][j].d-d[i][j].d);
	   }
    }

   calculateStateVar(pg1,tg1,ug1);

     //Step 2 
    calculateDummyCells(pg1, tg1, ug1);
    calculateF(pg1, tg1, ug1);
    calculateG(pg1, tg1, ug1);
    calculateR();

    //omp for 
    for (i = 1; i < imax; i++){
      for (j = 1; j < jmax; ++j) {
		ug1[i][j].a=
		  ug[i][j].a-0.33333*deltat[i][j]/aofTunnel[i][j]*(r[i][j].a-d[i][j].a);
		ug1[i][j].b=
		  ug[i][j].b-0.33333*deltat[i][j]/aofTunnel[i][j]*(r[i][j].b-d[i][j].b);
		ug1[i][j].c=
		  ug[i][j].c-0.33333*deltat[i][j]/aofTunnel[i][j]*(r[i][j].c-d[i][j].c);
		ug1[i][j].d=
		  ug[i][j].d-0.33333*deltat[i][j]/aofTunnel[i][j]*(r[i][j].d-d[i][j].d);
      }
    }
      
    calculateStateVar(pg1,tg1,ug1);
    
    // Step 3 
    calculateDummyCells(pg1, tg1, ug1);
    calculateF(pg1, tg1, ug1);
    calculateG(pg1, tg1, ug1);
    calculateR();

    //omp for 
    for (i = 1; i < imax; i++){
      for (j = 1; j < jmax; ++j) {
		ug1[i][j].a=
		  ug[i][j].a-0.5*deltat[i][j]/aofTunnel[i][j]*(r[i][j].a-d[i][j].a);
		ug1[i][j].b=
		  ug[i][j].b-0.5*deltat[i][j]/aofTunnel[i][j]*(r[i][j].b-d[i][j].b);
		ug1[i][j].c=
		  ug[i][j].c-0.5*deltat[i][j]/aofTunnel[i][j]*(r[i][j].c-d[i][j].c);
		ug1[i][j].d=
		  ug[i][j].d-0.5*deltat[i][j]/aofTunnel[i][j]*(r[i][j].d-d[i][j].d);

      }
    }

   //calculateStateVar(pg1, tg1, ug1);
    calculateStateVar(pg1,tg1,ug1);
    
    // Step 4 (final step) 
    calculateDummyCells(pg1, tg1, ug1);
    calculateF(pg1, tg1, ug1);
    calculateG(pg1, tg1, ug1);
    calculateR();
    //omp for 
    for (i = 1; i < imax; i++){
      for (j = 1; j < jmax; ++j) {
		ug[i][j].a -= deltat[i][j]/aofTunnel[i][j]*(r[i][j].a-d[i][j].a);
		ug[i][j].b -= deltat[i][j]/aofTunnel[i][j]*(r[i][j].b-d[i][j].b);
		ug[i][j].c -= deltat[i][j]/aofTunnel[i][j]*(r[i][j].c-d[i][j].c);
		ug[i][j].d -= deltat[i][j]/aofTunnel[i][j]*(r[i][j].d-d[i][j].d);
	   }
    }
    //calculateStateVar(pg, tg, ug);
	// OF PARALLEL
    error = 0.0;
    //omp parallel for private(scrap,j) reduction(+:double error)
    for (i = 1; i < imax; i++){
      for (j = 1; j < jmax; ++j) {
    	  scrap = pg[i][j]-opg[i][j];
    	  error += scrap*scrap;
      }
    }
      error = Math.sqrt(error / (double)((imax-1) * (jmax-1)) ); 

}
 /*Method Name = calculateStateVar
		  Vertex Name = pg1, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = pg, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = tg1, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = tg, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = ug1, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = ug, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = imax, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = jmax, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = b, Post Permissions = pure, Pre-Permissions =pure // associate object information
		  Vertex Name = c, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = d, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = a, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = Cv, Post Permissions = immutable, Pre-Permissions =immutable
		  Vertex Name = rgas, Post Permissions = immutable, Pre-Permissions =immutable*/
 
  /* @Perm(requires="pure(this) * full(#0) * full(#1) * pure(#2) in alive",
		  ensures="pure(this) * full(#0) * full(#1) * pure(#2) in alive")*/
  private void calculateStateVar(double localpg[][], double localtg[][],
			 Statevector localug[][])
 /* Calculates the new state values for range-kutta */
 /* Works for default values, 4/11 at 9:45 pm */
 {
   double temp, temp2;
   int j;

   //omp for 
   for (int i = 1; i < imax; i++) {
	for (j = 1; j < jmax; ++j) {
	  temp = localug[i][j].b;
	  temp2 = localug[i][j].c;
	  localtg[i][j] = localug[i][j].d/localug[i][j].a - 0.5 *
	    (temp*temp + temp2*temp2)/(localug[i][j].a*localug[i][j].a);

	  localtg[i][j] = localtg[i][j] / Cv;
	  localpg[i][j] = localug[i][j].a * rgas * localtg[i][j];
	}
   }
 }
  /*Method Name = calculateR
  Vertex Name = imax, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = jmax, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = a, Post Permissions = share, Pre-Permissions =share
  Vertex Name = r, Post Permissions = pure, Pre-Permissions =pure// it should be share else remove it, only include a, b,c,d
  Vertex Name = b, Post Permissions = share, Pre-Permissions =share
  Vertex Name = c, Post Permissions = share, Pre-Permissions =share
  Vertex Name = d, Post Permissions = share, Pre-Permissions =share
  Vertex Name = ynode, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = xnode, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = f, Post Permissions = pure, Pre-Permissions =pure // Permissions are fine else remove it,only include a, b,c,d
  Vertex Name = g, Post Permissions = pure, Pre-Permissions =pure*/ //Permissions are fine else remove it,only include a, b,c,d*/

private void calculateR() {

double deltax, deltay;
double temp;
int j;
Statevector scrap;

//omp for 
for (int i = 1; i < imax; i++) {
for (j = 1; j < jmax; ++j) {

/* Start by clearing R */
r[i][j].a = 0.0;
r[i][j].b = 0.0;
r[i][j].c = 0.0;
r[i][j].d = 0.0;

/* East Face */
deltay = (ynode[i][j] - ynode[i][j-1]);
deltax = (xnode[i][j] - xnode[i][j-1]);
temp = 0.5 * deltay;
r[i][j].a += temp*(f[i][j].a + f[i+1][j].a);
r[i][j].b += temp*(f[i][j].b + f[i+1][j].b);
r[i][j].c += temp*(f[i][j].c + f[i+1][j].c);
r[i][j].d += temp*(f[i][j].d + f[i+1][j].d);

temp = -0.5*deltax;
r[i][j].a += temp * (g[i][j].a+g[i+1][j].a);
r[i][j].b += temp * (g[i][j].b+g[i+1][j].b);
r[i][j].c += temp * (g[i][j].c+g[i+1][j].c);
r[i][j].d += temp * (g[i][j].d+g[i+1][j].d);

/* South Face */
deltay = (ynode[i][j-1] - ynode[i-1][j-1]);  
deltax = (xnode[i][j-1] - xnode[i-1][j-1]);

temp = 0.5 * deltay;
r[i][j].a  += temp*(f[i][j].a+f[i][j-1].a);
r[i][j].b  += temp*(f[i][j].b+f[i][j-1].b);
r[i][j].c  += temp*(f[i][j].c+f[i][j-1].c);
r[i][j].d  += temp*(f[i][j].d+f[i][j-1].d);

temp = -0.5*deltax;
r[i][j].a += temp * (g[i][j].a+g[i][j-1].a);
r[i][j].b += temp * (g[i][j].b+g[i][j-1].b);
r[i][j].c += temp * (g[i][j].c+g[i][j-1].c);
r[i][j].d += temp * (g[i][j].d+g[i][j-1].d);

/* West Face */
deltay = (ynode[i-1][j-1] - ynode[i-1][j]);
deltax = (xnode[i-1][j-1] - xnode[i-1][j]);

temp = 0.5 * deltay;
r[i][j].a  += temp*(f[i][j].a+f[i-1][j].a);
r[i][j].b  += temp*(f[i][j].b+f[i-1][j].b);
r[i][j].c  += temp*(f[i][j].c+f[i-1][j].c);
r[i][j].d  += temp*(f[i][j].d+f[i-1][j].d);

temp = -0.5*deltax;
r[i][j].a += temp * (g[i][j].a+g[i-1][j].a);
r[i][j].b += temp * (g[i][j].b+g[i-1][j].b);
r[i][j].c += temp * (g[i][j].c+g[i-1][j].c);
r[i][j].d += temp * (g[i][j].d+g[i-1][j].d);


/* North Face */
deltay = (ynode[i-1][j] - ynode[i][j]);
deltax = (xnode[i-1][j] - xnode[i][j]);

temp = 0.5 * deltay;
r[i][j].a  += temp*(f[i][j].a+f[i+1][j].a);
r[i][j].b  += temp*(f[i][j].b+f[i+1][j].b);
r[i][j].c  += temp*(f[i][j].c+f[i+1][j].c);
r[i][j].d  += temp*(f[i][j].d+f[i+1][j].d);

temp = -0.5*deltax;
r[i][j].a += temp * (g[i][j].a+g[i][j+1].a);
r[i][j].b += temp * (g[i][j].b+g[i][j+1].b);
r[i][j].c += temp * (g[i][j].c+g[i][j+1].c);
r[i][j].d += temp * (g[i][j].d+g[i][j+1].d);

}
}
}

/* Method Name = calculateG
  Vertex Name = pg, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = pg1, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = tg, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = tg1, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = ug, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = ug1, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = imax, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = jmax, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = c, Post Permissions = share, Pre-Permissions =share // associate object info
  Vertex Name = a, Post Permissions = share, Pre-Permissions =share// associate object info
  Vertex Name = g, Post Permissions = pure, Pre-Permissions =pure// should be share or remove it
  Vertex Name = b, Post Permissions = share, Pre-Permissions =share// associate object info
  Vertex Name = d, Post Permissions = share, Pre-Permissions =share// associate object info
  Vertex Name = Cp, Post Permissions = immutable, Pre-Permissions =immutable*/
private void calculateG(double localpg[][], double localtg[][],
  Statevector localug[][]) {
double temp, temp2, temp3;
double v;
int j;

//omp for 
for (int i = 0; i < imax + 1; i++) {
for (j = 0; j < jmax + 1; ++j) {
v = localug[i][j].c / localug[i][j].a;
g[i][j].a = localug[i][j].c;
g[i][j].b = localug[i][j].b * v;
g[i][j].c = localug[i][j].c*v + localpg[i][j];
temp = localug[i][j].b * localug[i][j].b;
temp2 = localug[i][j].c * localug[i][j].c;
temp3 = localug[i][j].a * localug[i][j].a;
g[i][j].d = localug[i][j].c * (Cp * localtg[i][j]+ 
 (0.5 * (temp + temp2)/(temp3)));
}
}
}

/*Method Name = calculateF
  Vertex Name = pg, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = pg1, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = tg, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = tg1, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = ug, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = ug1, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = imax, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = jmax, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = b, Post Permissions = share, Pre-Permissions =share // associate object info
  Vertex Name = a, Post Permissions = share, Pre-Permissions =share//associate object info
  Vertex Name = f, Post Permissions = pure, Pre-Permissions =pure // it should be share
  Vertex Name = c, Post Permissions = share, Pre-Permissions =share//associate object info
  Vertex Name = d, Post Permissions = share, Pre-Permissions =share//associate object info
  Vertex Name = Cp, Post Permissions = immutable, Pre-Permissions =immutable*/
private void calculateF(double localpg[][], double localtg[][], 
  Statevector localug[][]) {
{
double u;
double temp1, temp2, temp3;
int j;

//omp for 
for (int i = 0; i < imax + 1; i++) {
for (j = 0; j < jmax + 1; ++j) {	  
u = localug[i][j].b/ localug[i][j].a;
f[i][j].a = localug[i][j].b;
f[i][j].b = localug[i][j].b *u + localpg[i][j];
f[i][j].c = localug[i][j].c * u;
temp1 = localug[i][j].b * localug[i][j].b;
temp2 = localug[i][j].c * localug[i][j].c;
temp3 = localug[i][j].a * localug[i][j].a;
f[i][j].d = localug[i][j].b * (Cp * localtg[i][j] + 
  	 (0.5 * (temp1 + temp2)/(temp3)));
}
}
}
}
/* Method Name = calculateDamping
  Vertex Name = pg, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = ug, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = secondOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = secondOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = fourthOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = fourthOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = imax, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = jmax, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = sxi, Post Permissions = share, Pre-Permissions =share
  Vertex Name = seta, Post Permissions = share, Pre-Permissions =share
  Vertex Name = a, Post Permissions = share, Pre-Permissions =share // associate object info
  Vertex Name = deltat, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = b, Post Permissions = share, Pre-Permissions =share // associate object info
  Vertex Name = c, Post Permissions = share, Pre-Permissions =share // associate object info
  Vertex Name = d, Post Permissions = share, Pre-Permissions =share*/ // associate object info
private void calculateDamping(double localpg[][], Statevector localug[][]) {
double adt, sbar;
double nu2;
double nu4;
double tempdouble;
int ascrap, j;
Statevector temp = new Statevector();
Statevector temp2 = new Statevector();
Statevector scrap2 = new Statevector(), scrap4 = new Statevector();

nu2 = secondOrderDamping * secondOrderNormalizer;
nu4 = fourthOrderDamping * fourthOrderNormalizer;

for (int i = 1; i < imax; i++)
for (j = 1; j < jmax; ++j) {
sxi[i][j] = Math.abs(localpg[i+1][j] -
2.0 * localpg[i][j] + localpg[i-1][j])/ localpg[i][j];
seta[i][j] = Math.abs(localpg[i][j+1] -
   2.0 * localpg[i][j] + localpg[i][j-1]) / localpg[i][j];
}

for (int i = 1; i < imax; i++) {
for (j = 1; j < jmax; ++j) {

if (i > 1 && i < imax-1) {
adt = (aofTunnel[i][j] + aofTunnel[i+1][j]) / (deltat[i][j] + deltat[i+1][j]);
sbar = (sxi[i+1][j] + sxi[i][j]) * 0.5;
}
else {
adt = aofTunnel[i][j]/deltat[i][j];
sbar = sxi[i][j];
}
tempdouble = nu2*sbar*adt;
scrap2.a = tempdouble * (localug[i+1][j].a-localug[i][j].a);
scrap2.b = tempdouble * (localug[i+1][j].b-localug[i][j].b);
scrap2.c = tempdouble * (localug[i+1][j].c-localug[i][j].c);
scrap2.d = tempdouble * (localug[i+1][j].d-localug[i][j].d);

if (i > 1 && i < imax-1) {
temp = localug[i+2][j].svect(localug[i-1][j]);// return unique(result)

temp2.a = 3.0*(localug[i][j].a-localug[i+1][j].a);
temp2.b = 3.0*(localug[i][j].b-localug[i+1][j].b);
temp2.c = 3.0*(localug[i][j].c-localug[i+1][j].c);
temp2.d = 3.0*(localug[i][j].d-localug[i+1][j].d);

tempdouble = -nu4*adt;
scrap4.a = tempdouble*(temp.a+temp2.a);
scrap4.b = tempdouble*(temp.a+temp2.b);
scrap4.c = tempdouble*(temp.a+temp2.c);
scrap4.d = tempdouble*(temp.a+temp2.d);
}
else {
scrap4.a = 0.0;
scrap4.b = 0.0;
scrap4.c = 0.0;
scrap4.d = 0.0;
}

temp.a = scrap2.a + scrap4.a;
temp.b = scrap2.b + scrap4.b;
temp.c = scrap2.c + scrap4.c;
temp.d = scrap2.d + scrap4.d;
d[i][j] = temp;

if(i > 1 && i < imax-1) {
adt = (aofTunnel[i][j] + aofTunnel[i-1][j]) / (deltat[i][j] + deltat[i-1][j]);
sbar = (sxi[i][j] + sxi[i-1][j]) *0.5;
}
else {
adt = aofTunnel[i][j]/deltat[i][j];
sbar = sxi[i][j];
}

tempdouble = -nu2*sbar*adt;
scrap2.a = tempdouble * (localug[i][j].a-localug[i-1][j].a);
scrap2.b = tempdouble * (localug[i][j].b-localug[i-1][j].b);
scrap2.c = tempdouble * (localug[i][j].c-localug[i-1][j].c);
scrap2.d = tempdouble * (localug[i][j].d-localug[i-1][j].d);


if (i > 1 && i < imax-1) {
temp = localug[i+1][j].svect(localug[i-2][j]);
temp2.a = 3.0*(localug[i-1][j].a-localug[i][j].a);
temp2.b = 3.0*(localug[i-1][j].b-localug[i][j].b);
temp2.c = 3.0*(localug[i-1][j].c-localug[i][j].c);
temp2.d = 3.0*(localug[i-1][j].d-localug[i][j].d);

tempdouble = nu4*adt;
scrap4.a = tempdouble*(temp.a+temp2.a);
scrap4.b = tempdouble*(temp.a+temp2.b);
scrap4.c = tempdouble*(temp.a+temp2.c);
scrap4.d = tempdouble*(temp.a+temp2.d);
}
else {
scrap4.a = 0.0;
scrap4.b = 0.0;
scrap4.c = 0.0;
scrap4.d = 0.0;
}

d[i][j].a += scrap2.a + scrap4.a;
d[i][j].b += scrap2.b + scrap4.b;
d[i][j].c += scrap2.c + scrap4.c;
d[i][j].d += scrap2.d + scrap4.d;

if (j > 1 && j < jmax-1) {
adt = (aofTunnel[i][j] + aofTunnel[i][j+1]) / (deltat[i][j] + deltat[i][j+1]);
sbar = (seta[i][j] + seta[i][j+1]) * 0.5;
}
else {
adt = aofTunnel[i][j]/deltat[i][j];
sbar = seta[i][j];
}
tempdouble = nu2*sbar*adt;
scrap2.a = tempdouble * (localug[i][j+1].a-localug[i][j].a);
scrap2.b = tempdouble * (localug[i][j+1].b-localug[i][j].b);
scrap2.c = tempdouble * (localug[i][j+1].c-localug[i][j].c);
scrap2.d = tempdouble * (localug[i][j+1].d-localug[i][j].d);

if (j > 1 && j < jmax-1) {
temp = localug[i][j+2].svect(localug[i][j-1]);
temp2.a = 3.0*(localug[i][j].a-localug[i][j+1].a);
temp2.b = 3.0*(localug[i][j].b-localug[i][j+1].b);
temp2.c = 3.0*(localug[i][j].c-localug[i][j+1].c);
temp2.d = 3.0*(localug[i][j].d-localug[i][j+1].d);

tempdouble = -nu4*adt;
scrap4.a = tempdouble*(temp.a+temp2.a);
scrap4.b = tempdouble*(temp.a+temp2.b);
scrap4.c = tempdouble*(temp.a+temp2.c);
scrap4.d = tempdouble*(temp.a+temp2.d);
}
else {
scrap4.a = 0.0;
scrap4.b = 0.0;
scrap4.c = 0.0;
scrap4.d = 0.0;
}
d[i][j].a += scrap2.a + scrap4.a;
d[i][j].b += scrap2.b + scrap4.b;
d[i][j].c += scrap2.c + scrap4.c;
d[i][j].d += scrap2.d + scrap4.d;

if (j > 1 && j < jmax-1) {
adt = (aofTunnel[i][j] + aofTunnel[i][j-1]) / (deltat[i][j] + deltat[i][j-1]);
sbar = (seta[i][j] + seta[i][j-1]) * 0.5;
}
else {
adt = aofTunnel[i][j]/deltat[i][j];
sbar = seta[i][j];
}
tempdouble = -nu2*sbar*adt;
scrap2.a = tempdouble * (localug[i][j].a-localug[i][j-1].a);
scrap2.b = tempdouble * (localug[i][j].b-localug[i][j-1].b);
scrap2.c = tempdouble * (localug[i][j].c-localug[i][j-1].c);
scrap2.d = tempdouble * (localug[i][j].d-localug[i][j-1].d);

if (j > 1 && j < jmax-1) {
temp = localug[i][j+1].svect(localug[i][j-2]);
temp2.a = 3.0*(localug[i][j-1].a-localug[i][j].a);
temp2.b = 3.0*(localug[i][j-1].b-localug[i][j].b);
temp2.c = 3.0*(localug[i][j-1].c-localug[i][j].c);
temp2.d = 3.0*(localug[i][j-1].d-localug[i][j].d);

tempdouble = nu4*adt;
scrap4.a = tempdouble*(temp.a+temp2.a);
scrap4.b = tempdouble*(temp.a+temp2.b);
scrap4.c = tempdouble*(temp.a+temp2.c);
scrap4.d = tempdouble*(temp.a+temp2.d);
}
else {
scrap4.a = 0.0;
scrap4.b = 0.0;
scrap4.c = 0.0;
scrap4.d = 0.0;
}
d[i][j].a += scrap2.a + scrap4.a;
d[i][j].b += scrap2.b + scrap4.b;
d[i][j].c += scrap2.c + scrap4.c;
d[i][j].d += scrap2.d + scrap4.d;
}
}
}
/* Method Name = calculateDeltaT
  Vertex Name = imax, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = jmax, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = xnode, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = ynode, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = b, Post Permissions = pure, Pre-Permissions =pure// associate object info
  Vertex Name = ug, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = c, Post Permissions = pure, Pre-Permissions =pure// associate object info
  Vertex Name = gamma, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = rgas, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = tg, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = deltat, Post Permissions = share, Pre-Permissions =share
  Vertex Name = a, Post Permissions = pure, Pre-Permissions =pure // associate object information
  Vertex Name = ntime, Post Permissions = immutable, Pre-Permissions =immutable*/

private void calculateDeltaT() {
double xeta, yeta, xxi, yxi;            
int j;
double mint;
double c, q, r;
double safety_factor = 0.7;

for (int i = 1; i < imax; i++)
for (j = 1; j < jmax; ++j) {
xxi = (xnode[i][j] - xnode[i-1][j] 
                     + xnode[i][j-1] - xnode[i-1][j-1]) * 0.5;
yxi = (ynode[i][j] - ynode[i-1][j] 
                     + ynode[i][j-1] - ynode[i-1][j-1]) * 0.5;
xeta = (xnode[i][j] - xnode[i][j-1] 
                 + xnode[i-1][j] - xnode[i-1][j-1]) * 0.5;
yeta = (ynode[i][j] - ynode[i][j-1] 
                 + ynode[i-1][j] - ynode[i-1][j-1]) * 0.5;

q = (yeta * ug[i][j].b - xeta * ug[i][j].c);
r = (-yxi * ug[i][j].b + xxi * ug[i][j].c);
c = Math.sqrt (gamma * rgas * tg[i][j]);

deltat[i][j] = safety_factor * 2.8284 * aofTunnel[i][j] /

( (Math.abs(q) + Math.abs(r))/ug[i][j].a + c * 
Math.sqrt(xxi*xxi + yxi*yxi + xeta*xeta + yeta*yeta +
		  2.0 * Math.abs(xeta*xxi + yeta*yxi)));
}

{
if (ntime == 1) {
mint = 100000.0;
for (int i = 1; i < imax; i++)
for (j = 1; j < jmax; ++j)
if (deltat[i][j] < mint)
mint = deltat[i][j];

for (int i = 1; i < imax; i++)
for (j = 1; j < jmax; ++j)
deltat[i][j] = mint;
}
}
//omp barrier

}
/*Method Name = calculateDummyCells
 Vertex Name = pg, Post Permissions = share, Pre-Permissions =share
 Vertex Name = tg, Post Permissions = share, Pre-Permissions =share
 Vertex Name = ug, Post Permissions = share, Pre-Permissions =share
 
 Vertex Name = pg1, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = tg1, Post Permissions = pure, Pre-Permissions =pure
   Vertex Name = ug1, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = uff, Post Permissions = share, Pre-Permissions =share
  Vertex Name = machff, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = jplusff, Post Permissions = share, Pre-Permissions =share
  Vertex Name = gamma, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = cff, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = jminusff, Post Permissions = share, Pre-Permissions =share
  Vertex Name = imax, Post Permissions = share, Pre-Permissions =share // it should be pure
  Vertex Name = ihat, Post Permissions = share, Pre-Permissions =share
  Vertex Name = xnode, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = jhat, Post Permissions = share, Pre-Permissions =share
  Vertex Name = ynode, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = a, Post Permissions = share, Pre-Permissions =share // associate object information
  Vertex Name = b, Post Permissions = share, Pre-Permissions =share // associate object information
  Vertex Name = c, Post Permissions = share, Pre-Permissions =share // associate object information
  Vertex Name = d, Post Permissions = share, Pre-Permissions =share // associate object information
  Vertex Name = Cv, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = jmax, Post Permissions = share, Pre-Permissions =share //// it should be pure
  Vertex Name = rgas, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = rhoff, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = vff, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = tff, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = pff, Post Permissions = pure, Pre-Permissions =pure*/
private void calculateDummyCells(double localpg[][],
	   double localtg[][], Statevector localug[][]) {
double c;
double jminus;
double jplus;
double s;
double rho, temp, u, v;
double scrap, scrap2;
double theta;
double uprime;
int i, j;
Vector2 norm = new Vector2();
Vector2 tan = new Vector2();
Vector2 u1 = new Vector2();

uff = machff;
jplusff = uff + 2.0 / (gamma - 1.0) * cff;
jminusff = uff - 2.0 / (gamma - 1.0) * cff;

//omp for 
for (i = 1; i < imax; i++) {
tan.ihat = xnode[i][0] - xnode[i-1][0];
tan.jhat = ynode[i][0] - ynode[i-1][0];
norm.ihat = - (ynode[i][0] - ynode[i-1][0]);
norm.jhat = xnode[i][0] - xnode[i-1][0];

scrap = tan.magnitude();
tan.ihat = tan.ihat / scrap;
tan.jhat = tan.jhat / scrap;
scrap = norm.magnitude();
norm.ihat = norm.ihat / scrap;
norm.jhat = norm.jhat / scrap;

rho = localug[i][1].a;
localtg[i][0] = localtg[i][1];
u1.ihat = localug[i][1].b / rho;
u1.jhat = localug[i][1].c / rho;

u = u1.dot(tan) + u1.dot(norm) * tan.jhat /norm.jhat;
u = u / (tan.ihat - (norm.ihat * tan.jhat / norm.jhat));

v = - (u1.dot(norm) + u * norm.ihat) / norm.jhat;
localug[i][0] = localug[i][0];
localug[i][0].a = localug[i][1].a;
localug[i][0].b = rho * u;
localug[i][0].c = rho * v;
localug[i][0].d = rho * (Cv * localtg[i][0] + 0.5 * (u*u + v*v));
localpg[i][0] = localpg[i][1];

tan.ihat = xnode[i][jmax-1] - xnode[i-1][jmax-1];
tan.jhat = ynode[i][jmax-1] - ynode[i-1][jmax-1];
norm.ihat = ynode[i][jmax-1] - ynode[i-1][jmax-1];
norm.jhat = -(xnode[i][jmax-1] - xnode[i-1][jmax-1]);

scrap = tan.magnitude();
tan.ihat = tan.ihat / scrap;
tan.jhat = tan.jhat / scrap;
scrap = norm.magnitude();
norm.ihat = norm.ihat / scrap;
norm.jhat = norm.jhat / scrap;

rho = localug[i][jmax-1].a;
temp = localtg[i][jmax-1];
u1.ihat = localug[i][jmax-1].b / rho;
u1.jhat = localug[i][jmax-1].c / rho;

u = u1.dot(tan) + u1.dot(norm) * tan.jhat /norm.jhat;
u = u / (tan.ihat - (norm.ihat * tan.jhat / norm.jhat));

v = - (u1.dot(norm) + u * norm.ihat) / norm.jhat;

localug[i][jmax].a = localug[i][jmax-1].a;
localug[i][jmax].b = rho * u;
localug[i][jmax].c = rho * v;
localug[i][jmax].d = rho * (Cv * temp + 0.5 * (u*u + v*v));
localtg[i][jmax] = temp;
localpg[i][jmax] = localpg[i][jmax-1];
}

temp=0;	// just for JOMP

//omp for 
for (j = 1; j < jmax; ++j) {
norm.ihat = ynode[0][j-1] - ynode[0][j];
norm.jhat = xnode[0][j] - xnode[0][j-1];
scrap = norm.magnitude();
norm.ihat = norm.ihat / scrap;
norm.jhat = norm.jhat / scrap;
theta = Math.acos((ynode[0][j-1] - ynode[0][j]) / 
Math.sqrt((xnode[0][j] - xnode[0][j-1])*(xnode[0][j] - xnode[0][j-1]) 
+ (ynode[0][j-1] - ynode[0][j]) * (ynode[0][j-1] - ynode[0][j])));

u1.ihat = localug[1][j].b / localug[1][j].a;
u1.jhat = localug[1][j].c / localug[1][j].a;
uprime = u1.ihat * Math.cos(theta);
c = Math.sqrt(gamma * rgas * localtg[1][j]);
if (uprime < -c) {
localug[0][j].a = rhoff;
localug[0][j].b = rhoff * uff;
localug[0][j].c = rhoff * vff;
localug[0][j].d = rhoff * (Cv * tff + 0.5 * (uff*uff + vff*vff));
localtg[0][j] = tff;
localpg[0][j] = pff;
}
else if(uprime < 0.0) {
jminus = u1.ihat - 2.0/(gamma-1.0) * c;
s = Math.log(pff) - gamma * Math.log(rhoff);
v = vff;

u = (jplusff + jminus) / 2.0;
scrap = (jplusff - u) * (gamma-1.0) * 0.5;
localtg[0][j] = (1.0 / (gamma * rgas)) * scrap * scrap;
localpg[0][j] = Math.exp(s) / Math.pow((rgas * localtg[0][j]), gamma);
localpg[0][j] = Math.pow(localpg[0][j], 1.0 / (1.0 - gamma));

localug[0][j].a = localpg[0][j] / (rgas * localtg[0][j]);
localug[0][j].b = localug[0][j].a * u;
localug[0][j].c = localug[0][j].a * v;
localug[0][j].d = localug[0][j].a * (Cv * tff + 0.5 * (u*u + v*v));
}
else {
System.err.println("You have outflow at the inlet, which is not allowed.");
}

norm.ihat = ynode[0][j] - ynode[0][j-1];
norm.jhat = xnode[0][j-1] - xnode[0][j];
scrap = norm.magnitude();
norm.ihat = norm.ihat / scrap;
norm.jhat = norm.jhat / scrap;
scrap = xnode[0][j-1] - xnode[0][j];
scrap2 = ynode[0][j] - ynode[0][j-1];
theta = Math.acos((ynode[0][j] - ynode[0][j-1]) / 
       Math.sqrt(scrap*scrap + scrap2*scrap2));

u1.ihat = localug[imax-1][j].b / localug[imax-1][j].a;
u1.jhat = localug[imax-1][j].c / localug[imax-1][j].a;
uprime = u1.ihat * Math.cos(theta);
c = Math.sqrt(gamma * rgas * localtg[imax-1][j]);
if (uprime > c){
localug[imax][j].a = 2.0 * localug[imax-1][j].a - localug[imax-2][j].a;
localug[imax][j].b = 2.0 * localug[imax-1][j].b - localug[imax-2][j].b;
localug[imax][j].c = 2.0 * localug[imax-1][j].c - localug[imax-2][j].c;
localug[imax][j].d = 2.0 * localug[imax-1][j].d - localug[imax-2][j].d;
localpg[imax][j] = 2.0 * localpg[imax-1][j] - localpg[imax-2][j];
localtg[imax][j] = 2.0 * localtg[imax-1][j] - localtg[imax-2][j];
}
else if (uprime < c && uprime > 0) {
jplus = u1.ihat + 2.0/(gamma - 1) * c;
v = localug[imax-1][j].c / localug[imax-1][j].a;
s = Math.log(localpg[imax-1][j]) -
                     gamma * Math.log(localug[imax-1][j].a);

u = (jplus + jminusff) / 2.0;
scrap =(jplus - u)* (gamma-1.0) * 0.5;
localtg[imax][j] = (1.0 / (gamma * rgas)) * scrap * scrap;
localpg[imax][j] = Math.exp(s) / 
                   Math.pow((rgas * localtg[imax][j]), gamma);
localpg[imax][j] = Math.pow(localpg[imax][j], 1.0 / (1.0-gamma));
rho = localpg[imax][j]/ (rgas * localtg[imax][j]);

localug[imax][j].a = rho;
localug[imax][j].b = rho * u;
localug[imax][j].c = rho * v;
localug[imax][j].d = rho * (Cv * localtg[imax][j] + 0.5 * (u*u + v*v));

}
else if (uprime < -c) {
localug[0][j].a = rhoff;
localug[0][j].b = rhoff * uff;
localug[0][j].c = rhoff * vff;
localug[0][j].d = rhoff * (Cv * tff + 0.5 * (uff*uff + vff*vff));
localtg[0][j] = tff;
localpg[0][j] = pff;
}
else if(uprime < 0.0) {
jminus = u1.ihat - 2.0/(gamma-1.0) * c;
s = Math.log(pff) - gamma * Math.log(rhoff);
v = vff;

u = (jplusff + jminus) / 2.0;
scrap = (jplusff - u)* (gamma-1.0) * 0.5;
localtg[0][j] = (1.0 / (gamma * rgas)) * scrap * scrap;
localpg[0][j] = Math.exp(s) / Math.pow((rgas * localtg[0][j]), gamma);
localpg[0][j] = Math.pow(localpg[0][j], 1.0 / (1.0 - gamma));

localug[0][j].a = localpg[0][j] / (rgas * localtg[0][j]);
localug[0][j].b = localug[0][j].a * u;
localug[0][j].c = localug[0][j].a * v;
localug[0][j].d = localug[0][j].a * (Cv * tff + 0.5 * (u*u + v*v));
}
else {
System.err.println("You have inflow at the outlet, which is not allowed.");
}
} 
//omp master 
{
localug[0][0] = localug[1][0];
localug[imax][0] = localug[imax][1];
localug[0][jmax] = localug[1][jmax];
localug[imax][jmax] = localug[imax][jmax-1];
}
//omp barrier
}

  public void runiters(){
    for (int i = 0; i<iter; i++){
      doIteration();
    }
  }
  
  

}

class Statevector{
  double a;   /* Storage for Statevectors */
  double b;
  double c;
  double d;
  /*Method Name = Statevector
		  Vertex Name = a, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = b, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = c, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = d, Post Permissions = share, Pre-Permissions =share*/
  Statevector() {
    a = 0.0;
    b = 0.0;
    c = 0.0;
    d = 0.0;
  }

  /* Most of these vector manipulation routines are not used in this program */
  /* because I inlined them for speed.  I leave them here because they may */
  /* be useful in the future. */
 /* Method Name = amvect
		  Vertex Name = a, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = b, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = c, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = d, Post Permissions = share, Pre-Permissions =share*/
  //all should be pure
  public Statevector amvect(double m, Statevector that) {
    /* Adds statevectors multiplies the sum by scalar m */
    
	Statevector answer = new Statevector();

    answer.a = m * (this.a + that.a);
    answer.b = m * (this.b + that.b);
    answer.c = m * (this.c + that.c);
    answer.d = m * (this.d + that.d);

    return answer;
  }
 /* Method Name = avect
		  Vertex Name = a, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = b, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = c, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = d, Post Permissions = share, Pre-Permissions =share*/
  //all should be pure
  public Statevector avect(Statevector that) {
     Statevector answer = new Statevector();
    /* Adds two statevectors */
    answer.a = this.a + that.a;
    answer.b = this.b + that.b;
    answer.c = this.c + that.c;
    answer.d = this.d + that.d;

    return answer;
  }
 /* Method Name = mvect
		  Vertex Name = a, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = b, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = c, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = d, Post Permissions = share, Pre-Permissions =share*/
  //all should be pure
  public Statevector mvect(double m) {
     Statevector answer = new Statevector();
    /* Multiplies statevector scalar m */    
    answer.a = m * this.a;
    answer.b = m * this.b;
    answer.c = m * this.c;
    answer.d = m * this.d;

    return answer;
  }
 /* Method Name = svect
		  Vertex Name = a, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = b, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = c, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = d, Post Permissions = share, Pre-Permissions =share*/
//all should be pure
  public Statevector svect(Statevector that) {
     Statevector answer = new Statevector();
    /* Subtracts vector that from this */
    answer.a = this.a - that.a;
    answer.b = this.b - that.b;
    answer.c = this.c - that.c;
    answer.d = this.d - that.d;
    
    return answer;
  }

  /*Method Name = smvect
		  Vertex Name = a, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = b, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = c, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = d, Post Permissions = share, Pre-Permissions =share*/
//all should be pure
  public Statevector smvect(double m, Statevector that) {
     Statevector answer = new Statevector();
    /* Subtracts statevector that from this and multiplies the */
    /* result by scalar m */
    answer.a = m * (this.a - that.a);
    answer.b = m * (this.b - that.b);
    answer.c = m * (this.c - that.c);
    answer.d = m * (this.d - that.d);
    
    return answer;
  }
  /*public static void main(String argv[]){

	    //JGFInstrumentor.printHeader(3,0);

	    Tunnel eb = new Tunnel(); 
	   
	    try {
			eb.initialise();
		} catch (Exception e) {
			e.printStackTrace();
		}
	 
	  }*/
}

class Vector2
{
  double ihat;   /* Storage for 2-D vector */
  double jhat;
  /*Method Name = Vector2
		  Vertex Name = ihat, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = jhat, Post Permissions = share, Pre-Permissions =share*/
  Vector2() {
    ihat = 0.0;
    jhat = 0.0;
  }
 /* Method Name = magnitude
		  Vertex Name = ihat, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = jhat, Post Permissions = pure, Pre-Permissions =pure*/
  public double magnitude() {
    double mag;
    mag = Math.sqrt(this.ihat*this.ihat + this.jhat * this.jhat);
    return mag;
  }
  /*Method Name = dot
		  Vertex Name = ihat, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = jhat, Post Permissions = pure, Pre-Permissions =pure*/
  public double dot(Vector2 that) {
    /* Calculates dot product of two 2-d vector */
    double answer;
    
    answer = this.ihat * that.ihat + this.jhat * that.jhat;
    
    return answer;
  }
}


/*Class Name = JGFEulerBench
Method Name = JGFsetsize
Vertex Name = size, Post Permissions = share, Pre-Permissions =share
Method Name = JGFinitialise
Vertex Name = timers, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = on, Post Permissions = share, Pre-Permissions =share
Vertex Name = name, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = start_time, Post Permissions = share, Pre-Permissions =share
Vertex Name = scale, Post Permissions = share, Pre-Permissions =share
Vertex Name = datasizes, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = size, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = imaxin, Post Permissions = share, Pre-Permissions =share
Vertex Name = jmaxin, Post Permissions = share, Pre-Permissions =share
Vertex Name = oldval, Post Permissions = unique, Pre-Permissions =none
Vertex Name = nf, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = imax, Post Permissions = share, Pre-Permissions =share
Vertex Name = jmax, Post Permissions = share, Pre-Permissions =share
Vertex Name = newval, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltat, Post Permissions = unique, Pre-Permissions =none
Vertex Name = opg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = pg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = pg1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sxi, Post Permissions = unique, Pre-Permissions =none
Vertex Name = seta, Post Permissions = unique, Pre-Permissions =none
Vertex Name = tg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = tg1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ug, Post Permissions = unique, Pre-Permissions =none
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Vertex Name = f, Post Permissions = unique, Pre-Permissions =none
Vertex Name = g, Post Permissions = unique, Pre-Permissions =none
Vertex Name = r, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ug1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = xnode, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ynode, Post Permissions = unique, Pre-Permissions =none
Vertex Name = cff, Post Permissions = share, Pre-Permissions =share
Vertex Name = vff, Post Permissions = share, Pre-Permissions =share
Vertex Name = pff, Post Permissions = share, Pre-Permissions =share
Vertex Name = gamma, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = rhoff, Post Permissions = share, Pre-Permissions =share
Vertex Name = tff, Post Permissions = share, Pre-Permissions =share
Vertex Name = rgas, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = Cv, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = time, Post Permissions = share, Pre-Permissions =share
Vertex Name = calls, Post Permissions = share, Pre-Permissions =share
Method Name = startTimer
Vertex Name = timers, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = on, Post Permissions = share, Pre-Permissions =share
Vertex Name = name, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = start_time, Post Permissions = share, Pre-Permissions =share
Method Name = start
Vertex Name = on, Post Permissions = share, Pre-Permissions =share
Vertex Name = name, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = start_time, Post Permissions = share, Pre-Permissions =share
Method Name = initialise
Vertex Name = scale, Post Permissions = share, Pre-Permissions =share
Vertex Name = datasizes, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = size, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = imaxin, Post Permissions = share, Pre-Permissions =share
Vertex Name = jmaxin, Post Permissions = share, Pre-Permissions =share
Vertex Name = oldval, Post Permissions = unique, Pre-Permissions =none
Vertex Name = nf, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = imax, Post Permissions = share, Pre-Permissions =share
Vertex Name = jmax, Post Permissions = share, Pre-Permissions =share
Vertex Name = newval, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltat, Post Permissions = unique, Pre-Permissions =none
Vertex Name = opg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = pg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = pg1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sxi, Post Permissions = unique, Pre-Permissions =none
Vertex Name = seta, Post Permissions = unique, Pre-Permissions =none
Vertex Name = tg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = tg1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ug, Post Permissions = unique, Pre-Permissions =none
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Vertex Name = f, Post Permissions = unique, Pre-Permissions =none
Vertex Name = g, Post Permissions = unique, Pre-Permissions =none
Vertex Name = r, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ug1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = xnode, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ynode, Post Permissions = unique, Pre-Permissions =none
Vertex Name = cff, Post Permissions = share, Pre-Permissions =share
Vertex Name = vff, Post Permissions = share, Pre-Permissions =share
Vertex Name = pff, Post Permissions = share, Pre-Permissions =share
Vertex Name = gamma, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = rhoff, Post Permissions = share, Pre-Permissions =share
Vertex Name = tff, Post Permissions = share, Pre-Permissions =share
Vertex Name = rgas, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = Cv, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = stopTimer
Vertex Name = timers, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = time, Post Permissions = share, Pre-Permissions =share
Vertex Name = start_time, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = on, Post Permissions = share, Pre-Permissions =share
Vertex Name = name, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = calls, Post Permissions = share, Pre-Permissions =share
Method Name = stop
Vertex Name = time, Post Permissions = share, Pre-Permissions =share
Vertex Name = start_time, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = on, Post Permissions = share, Pre-Permissions =share
Vertex Name = name, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = calls, Post Permissions = share, Pre-Permissions =share
Method Name = JGFapplication
Vertex Name = timers, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = on, Post Permissions = share, Pre-Permissions =share
Vertex Name = name, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = start_time, Post Permissions = share, Pre-Permissions =share
Vertex Name = iter, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = imax, Post Permissions = share, Pre-Permissions =share
Vertex Name = jmax, Post Permissions = share, Pre-Permissions =share
Vertex Name = opg, Post Permissions = share, Pre-Permissions =share
Vertex Name = pg, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tg, Post Permissions = share, Pre-Permissions =share
Vertex Name = ug, Post Permissions = share, Pre-Permissions =share
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = ug1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltat, Post Permissions = share, Pre-Permissions =share
Vertex Name = r, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = pg1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tg1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = error, Post Permissions = share, Pre-Permissions =share
Vertex Name = uff, Post Permissions = share, Pre-Permissions =share
Vertex Name = machff, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = jplusff, Post Permissions = share, Pre-Permissions =share
Vertex Name = gamma, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = cff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = jminusff, Post Permissions = share, Pre-Permissions =share
Vertex Name = ihat, Post Permissions = share, Pre-Permissions =share
Vertex Name = xnode, Post Permissions = share, Pre-Permissions =share
Vertex Name = jhat, Post Permissions = share, Pre-Permissions =share
Vertex Name = ynode, Post Permissions = share, Pre-Permissions =share
Vertex Name = Cv, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = rgas, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = rhoff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = vff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = pff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ntime, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = secondOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = secondOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = fourthOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = fourthOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sxi, Post Permissions = share, Pre-Permissions =share
Vertex Name = seta, Post Permissions = share, Pre-Permissions =share
Vertex Name = f, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Cp, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = g, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = time, Post Permissions = share, Pre-Permissions =share
Vertex Name = calls, Post Permissions = share, Pre-Permissions =share
Method Name = runiters
Vertex Name = iter, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = imax, Post Permissions = share, Pre-Permissions =share
Vertex Name = jmax, Post Permissions = share, Pre-Permissions =share
Vertex Name = opg, Post Permissions = share, Pre-Permissions =share
Vertex Name = pg, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tg, Post Permissions = share, Pre-Permissions =share
Vertex Name = ug, Post Permissions = share, Pre-Permissions =share
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = ug1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltat, Post Permissions = share, Pre-Permissions =share
Vertex Name = r, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = pg1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tg1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = error, Post Permissions = share, Pre-Permissions =share
Vertex Name = uff, Post Permissions = share, Pre-Permissions =share
Vertex Name = machff, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = jplusff, Post Permissions = share, Pre-Permissions =share
Vertex Name = gamma, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = cff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = jminusff, Post Permissions = share, Pre-Permissions =share
Vertex Name = ihat, Post Permissions = share, Pre-Permissions =share
Vertex Name = xnode, Post Permissions = share, Pre-Permissions =share
Vertex Name = jhat, Post Permissions = share, Pre-Permissions =share
Vertex Name = ynode, Post Permissions = share, Pre-Permissions =share
Vertex Name = Cv, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = rgas, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = rhoff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = vff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = pff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ntime, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = secondOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = secondOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = fourthOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = fourthOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sxi, Post Permissions = share, Pre-Permissions =share
Vertex Name = seta, Post Permissions = share, Pre-Permissions =share
Vertex Name = f, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Cp, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = g, Post Permissions = pure, Pre-Permissions =pure
Method Name = doIteration
Vertex Name = imax, Post Permissions = share, Pre-Permissions =share
Vertex Name = jmax, Post Permissions = share, Pre-Permissions =share
Vertex Name = opg, Post Permissions = share, Pre-Permissions =share
Vertex Name = pg, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tg, Post Permissions = share, Pre-Permissions =share
Vertex Name = ug, Post Permissions = share, Pre-Permissions =share
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = ug1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltat, Post Permissions = share, Pre-Permissions =share
Vertex Name = r, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = pg1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tg1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = error, Post Permissions = share, Pre-Permissions =share
Vertex Name = uff, Post Permissions = share, Pre-Permissions =share
Vertex Name = machff, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = jplusff, Post Permissions = share, Pre-Permissions =share
Vertex Name = gamma, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = cff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = jminusff, Post Permissions = share, Pre-Permissions =share
Vertex Name = ihat, Post Permissions = share, Pre-Permissions =share
Vertex Name = xnode, Post Permissions = share, Pre-Permissions =share
Vertex Name = jhat, Post Permissions = share, Pre-Permissions =share
Vertex Name = ynode, Post Permissions = share, Pre-Permissions =share
Vertex Name = Cv, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = rgas, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = rhoff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = vff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = pff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ntime, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = secondOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = secondOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = fourthOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = fourthOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sxi, Post Permissions = share, Pre-Permissions =share
Vertex Name = seta, Post Permissions = share, Pre-Permissions =share
Vertex Name = f, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Cp, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = g, Post Permissions = pure, Pre-Permissions =pure
Method Name = calculateDummyCells
Vertex Name = pg1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tg1, Post Permissions = share, Pre-Permissions =share
Vertex Name = ug1, Post Permissions = share, Pre-Permissions =share
Vertex Name = uff, Post Permissions = share, Pre-Permissions =share
Vertex Name = machff, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = jplusff, Post Permissions = share, Pre-Permissions =share
Vertex Name = gamma, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = cff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = jminusff, Post Permissions = share, Pre-Permissions =share
Vertex Name = imax, Post Permissions = share, Pre-Permissions =share
Vertex Name = ihat, Post Permissions = share, Pre-Permissions =share
Vertex Name = xnode, Post Permissions = share, Pre-Permissions =share
Vertex Name = jhat, Post Permissions = share, Pre-Permissions =share
Vertex Name = ynode, Post Permissions = share, Pre-Permissions =share
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Vertex Name = Cv, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = jmax, Post Permissions = share, Pre-Permissions =share
Vertex Name = rgas, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = rhoff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = vff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = pff, Post Permissions = pure, Pre-Permissions =pure
Method Name = magnitude
Vertex Name = ihat, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = jhat, Post Permissions = pure, Pre-Permissions =pure
Method Name = dot
Vertex Name = ihat, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = jhat, Post Permissions = pure, Pre-Permissions =pure
Method Name = calculateDeltaT
Vertex Name = imax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = jmax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = xnode, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ynode, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = b, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ug, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = gamma, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = rgas, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = tg, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltat, Post Permissions = share, Pre-Permissions =share
Vertex Name = a, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ntime, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = calculateDamping
Vertex Name = pg, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ug, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = secondOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = secondOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = fourthOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = fourthOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = imax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = jmax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = sxi, Post Permissions = share, Pre-Permissions =share
Vertex Name = seta, Post Permissions = share, Pre-Permissions =share
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltat, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Method Name = svect
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Method Name = calculateF
Vertex Name = pg1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tg1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ug1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = imax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = jmax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = f, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Vertex Name = Cp, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = calculateG
Vertex Name = pg1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tg1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ug1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = imax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = jmax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = g, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Vertex Name = Cp, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = calculateR
Vertex Name = imax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = jmax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = r, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Vertex Name = ynode, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = xnode, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = f, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = g, Post Permissions = pure, Pre-Permissions =pure
Method Name = calculateStateVar
Vertex Name = pg, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tg, Post Permissions = share, Pre-Permissions =share
Vertex Name = ug, Post Permissions = share, Pre-Permissions =share
Vertex Name = imax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = jmax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = b, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = d, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = a, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Cv, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = rgas, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = JGFvalidate
Vertex Name = error, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = size, Post Permissions = pure, Pre-Permissions =pure
Method Name = JGFtidyup
Vertex Name = a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltat, Post Permissions = unique, Pre-Permissions =none
Vertex Name = opg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = pg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = pg1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sxi, Post Permissions = unique, Pre-Permissions =none
Vertex Name = seta, Post Permissions = unique, Pre-Permissions =none
Vertex Name = tg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = tg1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = xnode, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ynode, Post Permissions = unique, Pre-Permissions =none
Vertex Name = d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = f, Post Permissions = unique, Pre-Permissions =none
Vertex Name = g, Post Permissions = unique, Pre-Permissions =none
Vertex Name = r, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ug1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ug, Post Permissions = unique, Pre-Permissions =none
Method Name = JGFrun
Vertex Name = imax, Post Permissions = share, Pre-Permissions =share
Vertex Name = jmax, Post Permissions = share, Pre-Permissions =share
Vertex Name = iter, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = timers, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = size, Post Permissions = share, Pre-Permissions =share
Vertex Name = on, Post Permissions = share, Pre-Permissions =share
Vertex Name = name, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = start_time, Post Permissions = share, Pre-Permissions =share
Vertex Name = scale, Post Permissions = share, Pre-Permissions =share
Vertex Name = datasizes, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = imaxin, Post Permissions = share, Pre-Permissions =share
Vertex Name = jmaxin, Post Permissions = share, Pre-Permissions =share
Vertex Name = oldval, Post Permissions = unique, Pre-Permissions =none
Vertex Name = nf, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = newval, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltat, Post Permissions = unique, Pre-Permissions =none
Vertex Name = opg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = pg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = pg1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sxi, Post Permissions = unique, Pre-Permissions =none
Vertex Name = seta, Post Permissions = unique, Pre-Permissions =none
Vertex Name = tg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = tg1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ug, Post Permissions = unique, Pre-Permissions =none
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Vertex Name = f, Post Permissions = unique, Pre-Permissions =none
Vertex Name = g, Post Permissions = unique, Pre-Permissions =none
Vertex Name = r, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ug1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = xnode, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ynode, Post Permissions = unique, Pre-Permissions =none
Vertex Name = cff, Post Permissions = share, Pre-Permissions =share
Vertex Name = vff, Post Permissions = share, Pre-Permissions =share
Vertex Name = pff, Post Permissions = share, Pre-Permissions =share
Vertex Name = gamma, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = rhoff, Post Permissions = share, Pre-Permissions =share
Vertex Name = tff, Post Permissions = share, Pre-Permissions =share
Vertex Name = rgas, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = Cv, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = time, Post Permissions = share, Pre-Permissions =share
Vertex Name = calls, Post Permissions = share, Pre-Permissions =share
Vertex Name = error, Post Permissions = share, Pre-Permissions =share
Vertex Name = uff, Post Permissions = share, Pre-Permissions =share
Vertex Name = machff, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = jplusff, Post Permissions = share, Pre-Permissions =share
Vertex Name = jminusff, Post Permissions = share, Pre-Permissions =share
Vertex Name = ihat, Post Permissions = share, Pre-Permissions =share
Vertex Name = jhat, Post Permissions = share, Pre-Permissions =share
Vertex Name = ntime, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = secondOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = secondOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = fourthOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = fourthOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Cp, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = opcount, Post Permissions = share, Pre-Permissions =share
Vertex Name = opname, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = addTimer
Vertex Name = timers, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = addOpsToTimer
Vertex Name = timers, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = opcount, Post Permissions = share, Pre-Permissions =share
Method Name = addops
Vertex Name = opcount, Post Permissions = share, Pre-Permissions =share
Method Name = printTimer
Vertex Name = timers, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = opname, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = name, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = time, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = size, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = opcount, Post Permissions = pure, Pre-Permissions =pure
Method Name = print
Vertex Name = opname, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = name, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = time, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = size, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = opcount, Post Permissions = pure, Pre-Permissions =pure
Method Name = perf
Vertex Name = opcount, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = time, Post Permissions = pure, Pre-Permissions =pure
Class Name = JGFEulerBenchSizeA
Method Name = main
Vertex Name = imax, Post Permissions = unique, Pre-Permissions =none
Vertex Name = jmax, Post Permissions = unique, Pre-Permissions =none
Vertex Name = iter, Post Permissions = none, Pre-Permissions =none
Vertex Name = timers, Post Permissions = none, Pre-Permissions =none
Vertex Name = size, Post Permissions = unique, Pre-Permissions =none
Vertex Name = on, Post Permissions = unique, Pre-Permissions =none
Vertex Name = name, Post Permissions = none, Pre-Permissions =none
Vertex Name = start_time, Post Permissions = unique, Pre-Permissions =none
Vertex Name = scale, Post Permissions = unique, Pre-Permissions =none
Vertex Name = datasizes, Post Permissions = none, Pre-Permissions =none
Vertex Name = imaxin, Post Permissions = unique, Pre-Permissions =none
Vertex Name = jmaxin, Post Permissions = unique, Pre-Permissions =none
Vertex Name = oldval, Post Permissions = unique, Pre-Permissions =none
Vertex Name = nf, Post Permissions = none, Pre-Permissions =none
Vertex Name = newval, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltat, Post Permissions = unique, Pre-Permissions =none
Vertex Name = opg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = pg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = pg1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sxi, Post Permissions = unique, Pre-Permissions =none
Vertex Name = seta, Post Permissions = unique, Pre-Permissions =none
Vertex Name = tg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = tg1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ug, Post Permissions = unique, Pre-Permissions =none
Vertex Name = a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = f, Post Permissions = unique, Pre-Permissions =none
Vertex Name = g, Post Permissions = unique, Pre-Permissions =none
Vertex Name = r, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ug1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = xnode, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ynode, Post Permissions = unique, Pre-Permissions =none
Vertex Name = cff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = vff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = pff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = gamma, Post Permissions = none, Pre-Permissions =none
Vertex Name = rhoff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = tff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = rgas, Post Permissions = none, Pre-Permissions =none
Vertex Name = b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Cv, Post Permissions = none, Pre-Permissions =none
Vertex Name = time, Post Permissions = unique, Pre-Permissions =none
Vertex Name = calls, Post Permissions = unique, Pre-Permissions =none
Vertex Name = error, Post Permissions = unique, Pre-Permissions =none
Vertex Name = uff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = machff, Post Permissions = none, Pre-Permissions =none
Vertex Name = jplusff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = jminusff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ihat, Post Permissions = unique, Pre-Permissions =none
Vertex Name = jhat, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ntime, Post Permissions = none, Pre-Permissions =none
Vertex Name = secondOrderDamping, Post Permissions = none, Pre-Permissions =none
Vertex Name = secondOrderNormalizer, Post Permissions = none, Pre-Permissions =none
Vertex Name = fourthOrderDamping, Post Permissions = none, Pre-Permissions =none
Vertex Name = fourthOrderNormalizer, Post Permissions = none, Pre-Permissions =none
Vertex Name = Cp, Post Permissions = none, Pre-Permissions =none
Vertex Name = opcount, Post Permissions = unique, Pre-Permissions =none
Vertex Name = opname, Post Permissions = none, Pre-Permissions =none
Method Name = printHeader
Class Name = Tunnel
Method Name = main
Vertex Name = datasizes, Post Permissions = unique, Pre-Permissions =none
Method Name = initialise
Vertex Name = scale, Post Permissions = share, Pre-Permissions =share
Vertex Name = datasizes, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = size, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = imaxin, Post Permissions = share, Pre-Permissions =share
Vertex Name = jmaxin, Post Permissions = share, Pre-Permissions =share
Vertex Name = oldval, Post Permissions = unique, Pre-Permissions =none
Vertex Name = nf, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = imax, Post Permissions = share, Pre-Permissions =share
Vertex Name = jmax, Post Permissions = share, Pre-Permissions =share
Vertex Name = newval, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltat, Post Permissions = unique, Pre-Permissions =none
Vertex Name = opg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = pg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = pg1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sxi, Post Permissions = unique, Pre-Permissions =none
Vertex Name = seta, Post Permissions = unique, Pre-Permissions =none
Vertex Name = tg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = tg1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ug, Post Permissions = unique, Pre-Permissions =none
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Vertex Name = f, Post Permissions = unique, Pre-Permissions =none
Vertex Name = g, Post Permissions = unique, Pre-Permissions =none
Vertex Name = r, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ug1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = xnode, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ynode, Post Permissions = unique, Pre-Permissions =none
Vertex Name = cff, Post Permissions = share, Pre-Permissions =share
Vertex Name = vff, Post Permissions = share, Pre-Permissions =share
Vertex Name = pff, Post Permissions = share, Pre-Permissions =share
Vertex Name = gamma, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = rhoff, Post Permissions = share, Pre-Permissions =share
Vertex Name = tff, Post Permissions = share, Pre-Permissions =share
Vertex Name = rgas, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = Cv, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = doIteration
Vertex Name = imax, Post Permissions = share, Pre-Permissions =share
Vertex Name = jmax, Post Permissions = share, Pre-Permissions =share
Vertex Name = opg, Post Permissions = share, Pre-Permissions =share
Vertex Name = pg, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tg, Post Permissions = share, Pre-Permissions =share
Vertex Name = ug, Post Permissions = share, Pre-Permissions =share
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = ug1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltat, Post Permissions = share, Pre-Permissions =share
Vertex Name = r, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = pg1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tg1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = error, Post Permissions = share, Pre-Permissions =share
Vertex Name = uff, Post Permissions = share, Pre-Permissions =share
Vertex Name = machff, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = jplusff, Post Permissions = share, Pre-Permissions =share
Vertex Name = gamma, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = cff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = jminusff, Post Permissions = share, Pre-Permissions =share
Vertex Name = ihat, Post Permissions = share, Pre-Permissions =share
Vertex Name = xnode, Post Permissions = share, Pre-Permissions =share
Vertex Name = jhat, Post Permissions = share, Pre-Permissions =share
Vertex Name = ynode, Post Permissions = share, Pre-Permissions =share
Vertex Name = Cv, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = rgas, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = rhoff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = vff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = pff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ntime, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = secondOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = secondOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = fourthOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = fourthOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sxi, Post Permissions = share, Pre-Permissions =share
Vertex Name = seta, Post Permissions = share, Pre-Permissions =share
Vertex Name = f, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Cp, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = g, Post Permissions = pure, Pre-Permissions =pure
Method Name = calculateStateVar
Vertex Name = pg, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tg, Post Permissions = share, Pre-Permissions =share
Vertex Name = ug, Post Permissions = share, Pre-Permissions =share
Vertex Name = imax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = jmax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = b, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = d, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = a, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Cv, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = rgas, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = calculateR
Vertex Name = imax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = jmax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = r, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Vertex Name = ynode, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = xnode, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = f, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = g, Post Permissions = pure, Pre-Permissions =pure
Method Name = calculateG
Vertex Name = pg1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tg1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ug1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = imax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = jmax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = g, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Vertex Name = Cp, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = calculateF
Vertex Name = pg1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tg1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ug1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = imax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = jmax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = f, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Vertex Name = Cp, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = calculateDamping
Vertex Name = pg, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ug, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = secondOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = secondOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = fourthOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = fourthOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = imax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = jmax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = sxi, Post Permissions = share, Pre-Permissions =share
Vertex Name = seta, Post Permissions = share, Pre-Permissions =share
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltat, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Method Name = calculateDeltaT
Vertex Name = imax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = jmax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = xnode, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ynode, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = b, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ug, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = gamma, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = rgas, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = tg, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltat, Post Permissions = share, Pre-Permissions =share
Vertex Name = a, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ntime, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = calculateDummyCells
Vertex Name = pg1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tg1, Post Permissions = share, Pre-Permissions =share
Vertex Name = ug1, Post Permissions = share, Pre-Permissions =share
Vertex Name = uff, Post Permissions = share, Pre-Permissions =share
Vertex Name = machff, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = jplusff, Post Permissions = share, Pre-Permissions =share
Vertex Name = gamma, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = cff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = jminusff, Post Permissions = share, Pre-Permissions =share
Vertex Name = imax, Post Permissions = share, Pre-Permissions =share
Vertex Name = ihat, Post Permissions = share, Pre-Permissions =share
Vertex Name = xnode, Post Permissions = share, Pre-Permissions =share
Vertex Name = jhat, Post Permissions = share, Pre-Permissions =share
Vertex Name = ynode, Post Permissions = share, Pre-Permissions =share
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Vertex Name = Cv, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = jmax, Post Permissions = share, Pre-Permissions =share
Vertex Name = rgas, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = rhoff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = vff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = pff, Post Permissions = pure, Pre-Permissions =pure
Method Name = runiters
Vertex Name = iter, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = imax, Post Permissions = share, Pre-Permissions =share
Vertex Name = jmax, Post Permissions = share, Pre-Permissions =share
Vertex Name = opg, Post Permissions = share, Pre-Permissions =share
Vertex Name = pg, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tg, Post Permissions = share, Pre-Permissions =share
Vertex Name = ug, Post Permissions = share, Pre-Permissions =share
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = ug1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltat, Post Permissions = share, Pre-Permissions =share
Vertex Name = r, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = pg1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tg1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = error, Post Permissions = share, Pre-Permissions =share
Vertex Name = uff, Post Permissions = share, Pre-Permissions =share
Vertex Name = machff, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = jplusff, Post Permissions = share, Pre-Permissions =share
Vertex Name = gamma, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = cff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = jminusff, Post Permissions = share, Pre-Permissions =share
Vertex Name = ihat, Post Permissions = share, Pre-Permissions =share
Vertex Name = xnode, Post Permissions = share, Pre-Permissions =share
Vertex Name = jhat, Post Permissions = share, Pre-Permissions =share
Vertex Name = ynode, Post Permissions = share, Pre-Permissions =share
Vertex Name = Cv, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = rgas, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = rhoff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = vff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = pff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ntime, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = secondOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = secondOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = fourthOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = fourthOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sxi, Post Permissions = share, Pre-Permissions =share
Vertex Name = seta, Post Permissions = share, Pre-Permissions =share
Vertex Name = f, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Cp, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = g, Post Permissions = pure, Pre-Permissions =pure
Class Name = Statevector
Method Name = Statevector
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Method Name = amvect
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Method Name = avect
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Method Name = mvect
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Method Name = svect
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Method Name = smvect
Vertex Name = a, Post Permissions = share, Pre-Permissions =share
Vertex Name = b, Post Permissions = share, Pre-Permissions =share
Vertex Name = c, Post Permissions = share, Pre-Permissions =share
Vertex Name = d, Post Permissions = share, Pre-Permissions =share
Class Name = Vector2
Method Name = Vector2
Vertex Name = ihat, Post Permissions = share, Pre-Permissions =share
Vertex Name = jhat, Post Permissions = share, Pre-Permissions =share
Method Name = magnitude
Vertex Name = ihat, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = jhat, Post Permissions = pure, Pre-Permissions =pure
Method Name = dot
Vertex Name = ihat, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = jhat, Post Permissions = pure, Pre-Permissions =pure*/

////////////////////////////////////////////////////////////////////////
/*Class Name = Tunnel
Class Name = Statevector
Method Name = main
Vertex Name = Tunnel.datasizes, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.machff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.secondOrderDamping, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.fourthOrderDamping, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.ntime, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.iter, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.nf, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.Cp, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.Cv, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.gamma, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.rgas, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.fourthOrderNormalizer, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.secondOrderNormalizer, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.scale, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.datasizes, Post Permissions = none, Pre-Permissions =none
Vertex Name = eb.size, Post Permissions = none, Pre-Permissions =none
Vertex Name = eb.imaxin, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.jmaxin, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.oldval, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.nf, Post Permissions = none, Pre-Permissions =none
Vertex Name = eb.imax, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.jmax, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.newval, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.deltat, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.opg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.pg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.pg1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.sxi, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.seta, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.tg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.tg1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.aofTunnel, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.xnode, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ynode, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.f, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.g, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.r, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.ug, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.ug1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.cff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.vff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.pff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.gamma, Post Permissions = none, Pre-Permissions =none
Vertex Name = eb.rhoff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.tff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.rgas, Post Permissions = none, Pre-Permissions =none
Vertex Name = Statevector.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Statevector.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Statevector.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Statevector.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.Cv, Post Permissions = none, Pre-Permissions =none
Vertex Name = eb.d.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.d, Post Permissions = unique, Pre-Permissions =none
Method Name = initialise
Vertex Name = eb.scale, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.datasizes, Post Permissions = none, Pre-Permissions =none
Vertex Name = eb.size, Post Permissions = none, Pre-Permissions =none
Vertex Name = eb.imaxin, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.jmaxin, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.oldval, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.nf, Post Permissions = none, Pre-Permissions =none
Vertex Name = eb.imax, Post Permissions = share, Pre-Permissions =share
Vertex Name = eb.jmax, Post Permissions = share, Pre-Permissions =share
Vertex Name = eb.newval, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.deltat, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.opg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.pg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.pg1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.sxi, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.seta, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.tg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.tg1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.aofTunnel, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.xnode, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ynode, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.f, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.g, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.r, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.ug, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.ug1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.cff, Post Permissions = full, Pre-Permissions =full
Vertex Name = eb.vff, Post Permissions = full, Pre-Permissions =full
Vertex Name = eb.pff, Post Permissions = full, Pre-Permissions =full
Vertex Name = eb.gamma, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = eb.rhoff, Post Permissions = full, Pre-Permissions =full
Vertex Name = eb.tff, Post Permissions = full, Pre-Permissions =full
Vertex Name = eb.rgas, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Statevector.a, Post Permissions = share, Pre-Permissions =share
Vertex Name = eb.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Statevector.b, Post Permissions = share, Pre-Permissions =share
Vertex Name = eb.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Statevector.c, Post Permissions = share, Pre-Permissions =share
Vertex Name = eb.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Statevector.d, Post Permissions = share, Pre-Permissions =share
Vertex Name = eb.Cv, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = eb.d.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.d, Post Permissions = unique, Pre-Permissions =none
Method Name = Statevector
Vertex Name = eb.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.d, Post Permissions = unique, Pre-Permissions =none
Method Name = doIteration
Vertex Name = Tunnel.imax, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.jmax, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.opg, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.pg, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.tg, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.ug, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.a, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.ug1, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.a, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.deltat, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.aofTunnel, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.r, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.d, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.b, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.b, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.c, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.c, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.d, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.pg1, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.tg1, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.error, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.uff, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.machff, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.jplusff, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.gamma, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.cff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.jminusff, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.ihat, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.xnode, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.jhat, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.ynode, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.Cv, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.rgas, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.rhoff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.vff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.tff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.pff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Vector2.ihat, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Vector2.jhat, Post Permissions = unique, Pre-Permissions =none
Vertex Name = tan.ihat, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tan.jhat, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = u1.ihat, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = u1.jhat, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.ntime, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.secondOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.secondOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.fourthOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.fourthOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.sxi, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.seta, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = eb.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.f, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.Cp, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.g, Post Permissions = pure, Pre-Permissions =pure
Method Name = calculateDummyCells
Vertex Name = Tunnel.pg, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.pg1, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.tg, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.tg1, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.ug, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.ug1, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.uff, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.machff, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.jplusff, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.gamma, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.cff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.jminusff, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.imax, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.ihat, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.xnode, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.jhat, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.ynode, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.a, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.a, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.b, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Statevector.b, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.c, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Statevector.c, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.d, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.d, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.Cv, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.jmax, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.rgas, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.rhoff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.vff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.tff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.pff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Vector2.ihat, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Vector2.jhat, Post Permissions = unique, Pre-Permissions =none
Vertex Name = tan.ihat, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tan.jhat, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = u1.ihat, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = u1.jhat, Post Permissions = pure, Pre-Permissions =pure
Method Name = Vector2
Vertex Name = Vector2.ihat, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Vector2.jhat, Post Permissions = unique, Pre-Permissions =none
Method Name = magnitude
Vertex Name = tan.ihat, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tan.jhat, Post Permissions = pure, Pre-Permissions =pure
Method Name = dot
Vertex Name = u1.ihat, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = u1.jhat, Post Permissions = pure, Pre-Permissions =pure
Method Name = calculateDeltaT
Vertex Name = Tunnel.imax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.jmax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.xnode, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.ynode, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Statevector.b, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.ug, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.b, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Statevector.c, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.c, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.gamma, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.rgas, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.tg, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.deltat, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.aofTunnel, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Statevector.a, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.a, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.ntime, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = calculateDamping
Vertex Name = Tunnel.pg, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.ug, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.secondOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.secondOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.fourthOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.fourthOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.imax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.jmax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.sxi, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.seta, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.aofTunnel, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.deltat, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.a, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.a, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.b, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.b, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.c, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.c, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.d, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.d, Post Permissions = share, Pre-Permissions =share
Vertex Name = eb.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.d, Post Permissions = unique, Pre-Permissions =none
Method Name = svect
Vertex Name = Statevector.a, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.b, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.c, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.d, Post Permissions = share, Pre-Permissions =share
Vertex Name = eb.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.d, Post Permissions = unique, Pre-Permissions =none
Method Name = calculateF
Vertex Name = Tunnel.pg, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.pg1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.tg, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.tg1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.ug, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.ug1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.imax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.jmax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Statevector.b, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.b, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Statevector.a, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.a, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.f, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Statevector.c, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.c, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Statevector.d, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.d, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.Cp, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = calculateG
Vertex Name = Tunnel.pg, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.pg1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.tg, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.tg1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.ug, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.ug1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.imax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.jmax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Statevector.c, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.c, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Statevector.a, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.a, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.g, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Statevector.b, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.b, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Statevector.d, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.d, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.Cp, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = calculateR
Vertex Name = Tunnel.imax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.jmax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Statevector.a, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.r, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.a, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Statevector.b, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.b, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Statevector.c, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.c, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Statevector.d, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.d, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.ynode, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.xnode, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.f, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.g, Post Permissions = pure, Pre-Permissions =pure
Method Name = calculateStateVar
Vertex Name = Tunnel.pg1, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.pg, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.tg1, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.tg, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.ug1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.ug, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.imax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.jmax, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Statevector.b, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.b, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Statevector.c, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.c, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Statevector.d, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.d, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Statevector.a, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.a, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.Cv, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.rgas, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = runiters
Vertex Name = Tunnel.iter, Post Permissions = none, Pre-Permissions =none
Vertex Name = Tunnel.imax, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.jmax, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.opg, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.pg, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.tg, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.ug, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.a, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.ug1, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.a, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.deltat, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.aofTunnel, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.r, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.d, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.b, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.b, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.c, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.c, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.d, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.pg1, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.tg1, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.error, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.uff, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.machff, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.jplusff, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.gamma, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.cff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.jminusff, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.ihat, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.xnode, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.jhat, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.ynode, Post Permissions = share, Pre-Permissions =share
Vertex Name = Tunnel.Cv, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.rgas, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.rhoff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.vff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.tff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.pff, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Vector2.ihat, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Vector2.jhat, Post Permissions = unique, Pre-Permissions =none
Vertex Name = tan.ihat, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tan.jhat, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = u1.ihat, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = u1.jhat, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.ntime, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.secondOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.secondOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.fourthOrderDamping, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.fourthOrderNormalizer, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.sxi, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.seta, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = eb.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.f, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = Tunnel.Cp, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = Tunnel.g, Post Permissions = pure, Pre-Permissions =pure
Class Name = Statevector
Class Name = Statevector
Method Name = main
Vertex Name = Tunnel.datasizes, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.machff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.secondOrderDamping, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.fourthOrderDamping, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.ntime, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.iter, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.nf, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.Cp, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.Cv, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.gamma, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.rgas, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.fourthOrderNormalizer, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.secondOrderNormalizer, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.scale, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.datasizes, Post Permissions = none, Pre-Permissions =none
Vertex Name = eb.size, Post Permissions = none, Pre-Permissions =none
Vertex Name = eb.imaxin, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.jmaxin, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.oldval, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.nf, Post Permissions = none, Pre-Permissions =none
Vertex Name = eb.imax, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.jmax, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.newval, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.deltat, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.opg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.pg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.pg1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.sxi, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.seta, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.tg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.tg1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.aofTunnel, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.xnode, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ynode, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.f, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.g, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.r, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.ug, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.ug1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.cff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.vff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.pff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.gamma, Post Permissions = none, Pre-Permissions =none
Vertex Name = eb.rhoff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.tff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.rgas, Post Permissions = none, Pre-Permissions =none
Vertex Name = Statevector.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Statevector.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Statevector.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Statevector.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.Cv, Post Permissions = none, Pre-Permissions =none
Vertex Name = eb.d.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.d, Post Permissions = unique, Pre-Permissions =none
Method Name = Statevector
Vertex Name = eb.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.d, Post Permissions = unique, Pre-Permissions =none
Method Name = amvect
Vertex Name = Statevector.a, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.b, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.c, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.d, Post Permissions = share, Pre-Permissions =share
Vertex Name = eb.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.d, Post Permissions = unique, Pre-Permissions =none
Method Name = avect
Vertex Name = Statevector.a, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.b, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.c, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.d, Post Permissions = share, Pre-Permissions =share
Vertex Name = eb.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.d, Post Permissions = unique, Pre-Permissions =none
Method Name = mvect
Vertex Name = Statevector.a, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.b, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.c, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.d, Post Permissions = share, Pre-Permissions =share
Vertex Name = eb.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.d, Post Permissions = unique, Pre-Permissions =none
Method Name = svect
Vertex Name = Statevector.a, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.b, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.c, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.d, Post Permissions = share, Pre-Permissions =share
Vertex Name = eb.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.d, Post Permissions = unique, Pre-Permissions =none
Method Name = smvect
Vertex Name = Statevector.a, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.b, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.c, Post Permissions = share, Pre-Permissions =share
Vertex Name = Statevector.d, Post Permissions = share, Pre-Permissions =share
Vertex Name = eb.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.d, Post Permissions = unique, Pre-Permissions =none
Method Name = printHeader
Class Name = Vector2
Class Name = Statevector
Method Name = main
Vertex Name = Tunnel.datasizes, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.machff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.secondOrderDamping, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.fourthOrderDamping, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.ntime, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.iter, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.nf, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.Cp, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.Cv, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.gamma, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.rgas, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.fourthOrderNormalizer, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Tunnel.secondOrderNormalizer, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.scale, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.datasizes, Post Permissions = none, Pre-Permissions =none
Vertex Name = eb.size, Post Permissions = none, Pre-Permissions =none
Vertex Name = eb.imaxin, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.jmaxin, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.oldval, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.nf, Post Permissions = none, Pre-Permissions =none
Vertex Name = eb.imax, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.jmax, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.newval, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.deltat, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.opg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.pg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.pg1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.sxi, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.seta, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.tg, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.tg1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.aofTunnel, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.xnode, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ynode, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.f, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.g, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.r, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.ug, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.ug1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.cff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.vff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.pff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.gamma, Post Permissions = none, Pre-Permissions =none
Vertex Name = eb.rhoff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.tff, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.rgas, Post Permissions = none, Pre-Permissions =none
Vertex Name = Statevector.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Statevector.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Statevector.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Statevector.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.Cv, Post Permissions = none, Pre-Permissions =none
Vertex Name = eb.d.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.d.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.f.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.g.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.r.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug.d, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.a, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.b, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.c, Post Permissions = unique, Pre-Permissions =none
Vertex Name = eb.ug1.d, Post Permissions = unique, Pre-Permissions =none
Method Name = Vector2
Vertex Name = Vector2.ihat, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Vector2.jhat, Post Permissions = unique, Pre-Permissions =none
Method Name = magnitude
Vertex Name = tan.ihat, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = tan.jhat, Post Permissions = pure, Pre-Permissions =pure
Method Name = dot
Vertex Name = u1.ihat, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = u1.jhat, Post Permissions = pure, Pre-Permissions =pure
////////////////////////////////////////////////////////////////
 * 
 * Compilation Unit Names = Tunnel.java
Class Name = Tunnel

//////////////////////////////////////////////////////


//////////////////////////////////////////////////////

//////////////////////////////////////////////////////
Class Name = Statevector
Method Name = main
Ref-Var= section3.euler.Tunnel.datasizes, Pre-Permissions=none, Post Permissions=unique
Ref-Var= section3.euler.Tunnel.machff, Pre-Permissions=none, Post Permissions=unique
Ref-Var= section3.euler.Tunnel.secondOrderDamping, Pre-Permissions=none, Post Permissions=unique
Ref-Var= section3.euler.Tunnel.fourthOrderDamping, Pre-Permissions=none, Post Permissions=unique
Ref-Var= section3.euler.Tunnel.ntime, Pre-Permissions=none, Post Permissions=unique
Ref-Var= section3.euler.Tunnel.iter, Pre-Permissions=none, Post Permissions=unique
Ref-Var= section3.euler.Tunnel.nf, Pre-Permissions=none, Post Permissions=unique
Ref-Var= section3.euler.Tunnel.Cp, Pre-Permissions=none, Post Permissions=unique
Ref-Var= section3.euler.Tunnel.Cv, Pre-Permissions=none, Post Permissions=unique
Ref-Var= section3.euler.Tunnel.gamma, Pre-Permissions=none, Post Permissions=unique
Ref-Var= section3.euler.Tunnel.rgas, Pre-Permissions=none, Post Permissions=unique
Ref-Var= section3.euler.Tunnel.fourthOrderNormalizer, Pre-Permissions=none, Post Permissions=unique
Ref-Var= section3.euler.Tunnel.secondOrderNormalizer, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.scale, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.datasizes, Pre-Permissions=none, Post Permissions=none
Ref-Var= eb.size, Pre-Permissions=none, Post Permissions=none
Ref-Var= eb.imaxin, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.jmaxin, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.oldval, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.nf, Pre-Permissions=none, Post Permissions=none
Ref-Var= eb.imax, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.jmax, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.newval, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.deltat, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.opg, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.pg, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.pg1, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.sxi, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.seta, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.tg, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.tg1, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.ug, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.aofTunnel, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.d, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.f, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.g, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.r, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.ug1, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.xnode, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.ynode, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.d.d, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.f.f, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.g.g, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.r.r, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.ug.ug, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.ug1.ug1, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.cff, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.vff, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.pff, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.gamma, Pre-Permissions=none, Post Permissions=none
Ref-Var= eb.rhoff, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.tff, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.rgas, Pre-Permissions=none, Post Permissions=none
Ref-Var= section3.euler.Statevector.a, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.a, Pre-Permissions=none, Post Permissions=none
Ref-Var= section3.euler.Statevector.b, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.b, Pre-Permissions=none, Post Permissions=none
Ref-Var= section3.euler.Statevector.c, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.c, Pre-Permissions=none, Post Permissions=none
Ref-Var= section3.euler.Statevector.d, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.d, Pre-Permissions=none, Post Permissions=none
Ref-Var= eb.Cv, Pre-Permissions=none, Post Permissions=none
Ref-Var= section3.euler.Tunnel.oldval, Pre-Permissions=none, Post Permissions=none
Method Name = initialise
Ref-Var= eb.scale, Pre-Permissions=share, Post Permissions=share
Ref-Var= eb.datasizes, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= eb.size, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= eb.imaxin, Pre-Permissions=share, Post Permissions=share
Ref-Var= eb.jmaxin, Pre-Permissions=share, Post Permissions=share
Ref-Var= eb.oldval, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.nf, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= eb.imax, Pre-Permissions=share, Post Permissions=share
Ref-Var= eb.jmax, Pre-Permissions=share, Post Permissions=share
Ref-Var= eb.newval, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.deltat, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.opg, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.pg, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.pg1, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.sxi, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.seta, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.tg, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.tg1, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.ug, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.aofTunnel, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.d, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.f, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.g, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.r, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.ug1, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.xnode, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.ynode, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.d.d, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.f.f, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.g.g, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.r.r, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.ug.ug, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.ug1.ug1, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.cff, Pre-Permissions=share, Post Permissions=share
Ref-Var= eb.vff, Pre-Permissions=share, Post Permissions=share
Ref-Var= eb.pff, Pre-Permissions=share, Post Permissions=share
Ref-Var= eb.gamma, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= eb.rhoff, Pre-Permissions=share, Post Permissions=share
Ref-Var= eb.tff, Pre-Permissions=share, Post Permissions=share
Ref-Var= eb.rgas, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.a, Pre-Permissions=share, Post Permissions=share
Ref-Var= eb.a, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.b, Pre-Permissions=share, Post Permissions=share
Ref-Var= eb.b, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.c, Pre-Permissions=share, Post Permissions=share
Ref-Var= eb.c, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.d, Pre-Permissions=share, Post Permissions=share
Ref-Var= eb.d, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= eb.Cv, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.oldval, Pre-Permissions=pure, Post Permissions=pure
Method Name = Statevector
Ref-Var= eb.d.a, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.d.b, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.d.c, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.d.d, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.f.a, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.f.b, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.f.c, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.f.d, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.g.a, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.g.b, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.g.c, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.g.d, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.r.a, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.r.b, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.r.c, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.r.d, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.ug.a, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.ug.b, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.ug.c, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.ug.d, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.ug1.a, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.ug1.b, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.ug1.c, Pre-Permissions=none, Post Permissions=unique
Ref-Var= eb.ug1.d, Pre-Permissions=none, Post Permissions=unique
Method Name = doIteration
Ref-Var= section3.euler.Tunnel.imax, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.jmax, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.opg, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.pg, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.tg, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.ug, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Statevector.a, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.ug1, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.a, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.deltat, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.aofTunnel, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.r, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.d, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.b, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.b, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.c, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.c, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.d, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.d, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.pg1, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.tg1, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.error, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.uff, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.machff, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.jplusff, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.gamma, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.cff, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.jminusff, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.ihat, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.xnode, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.jhat, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.ynode, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.Cv, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.rgas, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.rhoff, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.vff, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.tff, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.pff, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= tan.ihat, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= tan.jhat, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= u1.ihat, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= u1.jhat, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.ntime, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.secondOrderDamping, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.secondOrderNormalizer, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.fourthOrderDamping, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.fourthOrderNormalizer, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.sxi, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.seta, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.f, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.Cp, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.g, Pre-Permissions=pure, Post Permissions=pure
Method Name = calculateDummyCells
Ref-Var= section3.euler.Tunnel.pg, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.pg1, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.tg, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.tg1, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.ug, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.ug1, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.uff, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.machff, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.jplusff, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.gamma, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.cff, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.jminusff, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.imax, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.ihat, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.xnode, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.jhat, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.ynode, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Statevector.a, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.a, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.b, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.b, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.c, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.c, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.d, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.d, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.Cv, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.jmax, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.rgas, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.rhoff, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.vff, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.tff, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.pff, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= tan.ihat, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= tan.jhat, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= u1.ihat, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= u1.jhat, Pre-Permissions=pure, Post Permissions=pure
Method Name = magnitude
Ref-Var= tan.ihat, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= tan.jhat, Pre-Permissions=pure, Post Permissions=pure
Method Name = dot
Ref-Var= u1.ihat, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= u1.jhat, Pre-Permissions=pure, Post Permissions=pure
Method Name = calculateDeltaT
Ref-Var= section3.euler.Tunnel.imax, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.jmax, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.xnode, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.ynode, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.b, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.ug, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.b, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.c, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.c, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.gamma, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.rgas, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.tg, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.deltat, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.aofTunnel, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.a, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.a, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.ntime, Pre-Permissions=pure, Post Permissions=pure
Method Name = calculateDamping
Ref-Var= section3.euler.Tunnel.pg, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.ug, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.secondOrderDamping, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.secondOrderNormalizer, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.fourthOrderDamping, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.fourthOrderNormalizer, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.imax, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.jmax, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.sxi, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.seta, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.aofTunnel, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.deltat, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.a, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.a, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.b, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.b, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.c, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.c, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.d, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.d, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.d, Pre-Permissions=share, Post Permissions=share
Method Name = svect
Ref-Var= section3.euler.Statevector.a, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.b, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.c, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.d, Pre-Permissions=pure, Post Permissions=pure
Method Name = calculateF
Ref-Var= section3.euler.Tunnel.pg, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.pg1, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.tg, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.tg1, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.ug, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.ug1, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.imax, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.jmax, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.b, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.b, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.a, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.a, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.f, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.c, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.c, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.d, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.d, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.Cp, Pre-Permissions=pure, Post Permissions=pure
Method Name = calculateG
Ref-Var= section3.euler.Tunnel.pg, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.pg1, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.tg, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.tg1, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.ug, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.ug1, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.imax, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.jmax, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.c, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.c, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.a, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.a, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.g, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.b, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.b, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.d, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.d, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.Cp, Pre-Permissions=pure, Post Permissions=pure
Method Name = calculateR
Ref-Var= section3.euler.Tunnel.imax, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.jmax, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.a, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.r, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.a, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.b, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.b, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.c, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.c, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.d, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.d, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.ynode, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.xnode, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.f, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.g, Pre-Permissions=pure, Post Permissions=pure
Method Name = calculateStateVar
Ref-Var= section3.euler.Tunnel.pg1, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.pg, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.tg1, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.tg, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.ug1, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.ug, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.imax, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.jmax, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.b, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.b, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.c, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.c, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.d, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.d, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.a, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.a, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.Cv, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.rgas, Pre-Permissions=pure, Post Permissions=pure
Method Name = runiters
Ref-Var= section3.euler.Tunnel.iter, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.imax, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.jmax, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.opg, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.pg, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.tg, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.ug, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Statevector.a, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.ug1, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.a, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.deltat, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.aofTunnel, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.r, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.d, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.b, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.b, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.c, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.c, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.d, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.d, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.pg1, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.tg1, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.error, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.uff, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.machff, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.jplusff, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.gamma, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.cff, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.jminusff, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.ihat, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.xnode, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.jhat, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.ynode, Pre-Permissions=share, Post Permissions=share
Ref-Var= section3.euler.Tunnel.Cv, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.rgas, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.rhoff, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.vff, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.tff, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.pff, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= tan.ihat, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= tan.jhat, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= u1.ihat, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= u1.jhat, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.ntime, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.secondOrderDamping, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.secondOrderNormalizer, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.fourthOrderDamping, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.fourthOrderNormalizer, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.sxi, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.seta, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.f, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.Cp, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Tunnel.g, Pre-Permissions=pure, Post Permissions=pure
Method Name = amvect
Ref-Var= section3.euler.Statevector.a, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.b, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.c, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.d, Pre-Permissions=pure, Post Permissions=pure
Method Name = avect
Ref-Var= section3.euler.Statevector.a, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.b, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.c, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.d, Pre-Permissions=pure, Post Permissions=pure
Method Name = mvect
Ref-Var= section3.euler.Statevector.a, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.b, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.c, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.d, Pre-Permissions=pure, Post Permissions=pure
Method Name = smvect
Ref-Var= section3.euler.Statevector.a, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.b, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.c, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= section3.euler.Statevector.d, Pre-Permissions=pure, Post Permissions=pure
Method Name = printHeader

//////////////////////////////////////////////////////


//////////////////////////////////////////////////////

//////////////////////////////////////////////////////
Class Name = Vector2
Method Name = Vector2
Ref-Var= section3.euler.Vector2.ihat, Pre-Permissions=none, Post Permissions=unique
Ref-Var= section3.euler.Vector2.jhat, Pre-Permissions=none, Post Permissions=unique
Method Name = magnitude
Ref-Var= tan.ihat, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= tan.jhat, Pre-Permissions=pure, Post Permissions=pure
Method Name = dot
Ref-Var= u1.ihat, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= u1.jhat, Pre-Permissions=pure, Post Permissions=pure

//////////////////////////////////////////////////////


//////////////////////////////////////////////////////

//////////////////////////////////////////////////////
 Milli Seconds Time = 6429.411801

*/
