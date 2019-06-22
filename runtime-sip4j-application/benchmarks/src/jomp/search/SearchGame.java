
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
*                     John Tromp (tromp@cwi.nl)                           *
*                   (see copyright notice below)                          *
*                                                                         *
*      This version copyright (c) The University of Edinburgh, 1999.      *
*                         All rights reserved.                            *
*                                                                         *
**************************************************************************/


// Java Fhourstones 2.0 connect-4 solver
// (http://www.cwi.nl/~tromp/c4/fhour.html)
//
// implementation of the well-known game
// played on a vertical board of 7 columns by 6 rows,
// where 2 players take turns in dropping counters in a column.
// the first player to get four of his counters
// in a horizontal, vertical or diagonal row, wins the game.
// if neither player has won after 42 moves, then the game is drawn.
//
// This software is copyright (c) 1996 by
//      John Tromp
//      Lindenlaan 33
//      1701 GT Heerhugowaard
//      Netherlands
// E-mail: tromp@cwi.nl
//
// This notice must not be removed.
// This software must not be sold for profit.
// You may redistribute if your distributees have the
// same rights and restrictions.

package jomp.search; 
import jomp.jgfutil.*; 

public class SearchGame extends TransGame {

  int history[][] = { {-1,-1,-1,-1,-1,-1,-1,-1,
		-1, 0, 1, 2, 4, 2, 1, 0,
		-1, 1, 3, 5, 7, 5, 3, 1,
		-1, 2, 5, 8,10, 8, 5, 2,
		-1, 2, 5, 8,10, 8, 5, 2,
		-1, 1, 3, 5, 7, 5, 3, 1,
		-1, 0, 1, 2, 4, 2, 1, 0},
		    {-1,-1,-1,-1,-1,-1,-1,-1,
		-1, 0, 1, 2, 4, 2, 1, 0,
		-1, 1, 3, 5, 7, 5, 3, 1,
		-1, 2, 5, 8,10, 8, 5, 2,
		-1, 2, 5, 8,10, 8, 5, 2,
		-1, 1, 3, 5, 7, 5, 3, 1,
		-1, 0, 1, 2, 4, 2, 1, 0}};

  long nodes, msecs;
  /*Method Name = SearchGame
		  Vertex Name = ht, Post Permissions = unique, Pre-Permissions =none
		  Vertex Name = TRANSIZE, Post Permissions = immutable, Pre-Permissions =immutable
		  Vertex Name = he, Post Permissions = unique, Pre-Permissions =none
		  Vertex Name = moves, Post Permissions = unique, Pre-Permissions =none
		  Vertex Name = rows, Post Permissions = unique, Pre-Permissions =none
		  Vertex Name = dias, Post Permissions = unique, Pre-Permissions =none
		  Vertex Name = columns, Post Permissions = unique, Pre-Permissions =none
		  Vertex Name = height, Post Permissions = unique, Pre-Permissions =none
		  Vertex Name = plycnt, Post Permissions = share, Pre-Permissions =share*/
  public SearchGame () {
    //super();
  }

  /*Method Name = solve
  Vertex Name = nodes, Post Permissions = share, Pre-Permissions =share
  Vertex Name = msecs, Post Permissions = share, Pre-Permissions =share
  Vertex Name = plycnt, Post Permissions = share, Pre-Permissions =share 
  Vertex Name = height, Post Permissions = share, Pre-Permissions =share 
  Vertex Name = colthr, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = columns, Post Permissions = share, Pre-Permissions =share
  Vertex Name = WIN, Post Permissions = share, Pre-Permissions =share
  Vertex Name = LOSE, Post Permissions = share, Pre-Permissions =share
  Vertex Name = ABSENT, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = posed, Post Permissions = share, Pre-Permissions =share 
  Vertex Name = rows, Post Permissions = share, Pre-Permissions =share
  Vertex Name = dias, Post Permissions = share, Pre-Permissions =share
  Vertex Name = htindex, Post Permissions = share, Pre-Permissions =share
  Vertex Name = PROBES, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = ht, Post Permissions = share, Pre-Permissions =share
  Vertex Name = lock, Post Permissions = share, Pre-Permissions =share
  Vertex Name = he, Post Permissions = share, Pre-Permissions =share
  Vertex Name = stride, Post Permissions = share, Pre-Permissions =share
  Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = timers, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = on, Post Permissions = share, Pre-Permissions =share
  Vertex Name = name, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = start_time, Post Permissions = share, Pre-Permissions =share
  Vertex Name = DRAW, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = DRAWLOSE, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = DRAWWIN, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = history, Post Permissions = share, Pre-Permissions =share
  Vertex Name = moves, Post Permissions = share, Pre-Permissions =share
  Vertex Name = hits, Post Permissions = share, Pre-Permissions =share /// should be share
  Vertex Name = time, Post Permissions = share, Pre-Permissions =share
  Vertex Name = calls, Post Permissions = immutable, Pre-Permissions =immutable*/
  int solve()
  {
    int i,side;
    int x,work,score;
    long poscnt;
  
    nodes = 0L; // nodes = rw
    msecs = 1L;// msec = rw
    side = (plycnt+1) & 1;//plycnt =r
    for (i = 0; ++i <= 7 ; )
      if (height[i] <= 6) {//height = r
        if (wins(i, height[i], 1<<side) || colthr[columns[i]] == (1<<side)) // colthr = r, coumns = r
        	//Vertex Name = rows, Post Permissions = share, Pre-Permissions =share
        	  /*Vertex Name = dias, Post Permissions = share, Pre-Permissions =share // both of these permissions should be be pure*/
        	 
          return (side!=0  ? WIN : LOSE) << 5;	// all score, no work:)// WIN = r, LOSe = r
      }
    if ((x = transpose()) != ABSENT) { //ABSENT = r
    	/*Method Name = transpose
		  Vertex Name = htindex, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = PROBES, Post Permissions = immutable, Pre-Permissions =immutable
		  Vertex Name = ht, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = lock, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = he, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = stride, Post Permissions = share, Pre-Permissions =share
		  Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = ABSENT, Post Permissions = immutable, Pre-Permissions =immutable
		  Vertex Name = columns, Post Permissions = pure, Pre-Permissions =pure
		  Vertex Name = NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
		  Vertex Name = STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
		  Vertex Name = INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable*/
      if ((x & 32) == 0)   // exact score
        return x;
    }
    //JGFInstrumentor.startTimer("Section3:AlphaBetaSearch:Run"); 
    /*Method Name = ab
    Vertex Name = LOSE, Post Permissions = share, Pre-Permissions =share
    Vertex Name = WIN, Post Permissions = share, Pre-Permissions =share
    Vertex Name = nodes, Post Permissions = pure, Pre-Permissions =pure
     Vertex Name = plycnt, Post Permissions = share, Pre-Permissions =share // It should be share check for getFieldOperation(exp)
    Vertex Name = DRAW, Post Permissions = immutable, Pre-Permissions =immutable
    Vertex Name = height, Post Permissions = share, Pre-Permissions =share // its should be share
    Vertex Name = colthr, Post Permissions = immutable, Pre-Permissions =immutable
    Vertex Name = columns, Post Permissions = share, Pre-Permissions =share
    Vertex Name = ABSENT, Post Permissions = immutable, Pre-Permissions =immutable
    Vertex Name = DRAWLOSE, Post Permissions = immutable, Pre-Permissions =immutable
    Vertex Name = DRAWWIN, Post Permissions = immutable, Pre-Permissions =immutable
    Vertex Name = posed, Post Permissions = share, Pre-Permissions =share// its should be share
    Vertex Name = history, Post Permissions = share, Pre-Permissions =share
    Vertex Name = rows, Post Permissions = share, Pre-Permissions =share
    Vertex Name = dias, Post Permissions = share, Pre-Permissions =share
    Vertex Name = moves, Post Permissions = share, Pre-Permissions =share
    Vertex Name = htindex, Post Permissions = share, Pre-Permissions =share
    Vertex Name = PROBES, Post Permissions = immutable, Pre-Permissions =immutable
    Vertex Name = ht, Post Permissions = share, Pre-Permissions =share
    Vertex Name = lock, Post Permissions = share, Pre-Permissions =share
    Vertex Name = he, Post Permissions = share, Pre-Permissions =share
    Vertex Name = stride, Post Permissions = share, Pre-Permissions =share
    Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
    Vertex Name = NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
    Vertex Name = STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
    Vertex Name = INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
    Vertex Name = hits, Post Permissions = share, Pre-Permissions =share*/// should be share
    poscnt = posed;// posed = r
    for (work=1; (poscnt>>=1) != 0; work++) ; //work = log of #positions stored 
    //JGFInstrumentor.stopTimer("Section3:AlphaBetaSearch:Run"); 
    score = ab(LOSE,WIN);//
    return score << 5 | work;
  }

  /*Method Name = ab
  Vertex Name = LOSE, Post Permissions = share, Pre-Permissions =share
  Vertex Name = WIN, Post Permissions = share, Pre-Permissions =share
  Vertex Name = nodes, Post Permissions = pure, Pre-Permissions =pure
   Vertex Name = plycnt, Post Permissions = share, Pre-Permissions =share // It should be share check for getFieldOperation(exp)
  Vertex Name = DRAW, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = height, Post Permissions = share, Pre-Permissions =share // its should be share
  Vertex Name = colthr, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = columns, Post Permissions = share, Pre-Permissions =share
  Vertex Name = ABSENT, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = DRAWLOSE, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = DRAWWIN, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = posed, Post Permissions = share, Pre-Permissions =share// its should be share
  Vertex Name = history, Post Permissions = share, Pre-Permissions =share
  Vertex Name = rows, Post Permissions = share, Pre-Permissions =share
  Vertex Name = dias, Post Permissions = share, Pre-Permissions =share
  Vertex Name = moves, Post Permissions = share, Pre-Permissions =share
  Vertex Name = htindex, Post Permissions = share, Pre-Permissions =share
  Vertex Name = PROBES, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = ht, Post Permissions = share, Pre-Permissions =share
  Vertex Name = lock, Post Permissions = share, Pre-Permissions =share
  Vertex Name = he, Post Permissions = share, Pre-Permissions =share
  Vertex Name = stride, Post Permissions = share, Pre-Permissions =share
  Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
  Vertex Name = NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
  Vertex Name = hits, Post Permissions = share, Pre-Permissions =share*/// should be share
  ///////////////////////////////////////////////////////////////////////////////////////////////
		
  int ab(int alpha, int beta)
  {
    int besti,i,j,h,k,l,val,score;
    int x,v,work;
    int nav =0, av[] = new int[8];
    long poscnt;
    int side = 0, otherside;
  
    nodes++; // ndoes = rw
    if (plycnt == 41) // plycntplycnt = r
      return DRAW; // DRAW = r
    side = (otherside = plycnt & 1) ^ 1;
    for (i = nav = 0; ++i <= 7;) {
      if ((h = height[i]) <= 6) { // height = r
        if (wins(i,h,3) || colthr[columns[i]] != 0) {//colthr = r, columns = r
          if (h+1 <= 6 && wins(i,h+1,1<<otherside))
            return LOSE;		// for 'o'
          av[0] = i;		// forget other moves
          while (++i <= 7)
            if ((h = height[i]) <= 6 &&
                (wins(i,h,3) || colthr[columns[i]] != 0))
              return LOSE;// LOSE = r
          nav = 1;
          break;
        }
        if (!(h+1 <= 6 && wins(i,h+1,1<<otherside))) //
    	av[nav++] = i;
      }
    }
    if (nav == 0)
      return LOSE;
    if (nav == 1) {
      
     makemove(av[0]);
     /*Method Name = makemove
	  Vertex Name = moves, Post Permissions = share, Pre-Permissions =share
	  Vertex Name = plycnt, Post Permissions = share, Pre-Permissions =share // It should be share check for getFieldOperation(exp)
	  Vertex Name = height, Post Permissions = pure, Pre-Permissions =pure
	  Vertex Name = columns, Post Permissions = share, Pre-Permissions =share
	  Vertex Name = rows, Post Permissions = share, Pre-Permissions =share
	  Vertex Name = dias, Post Permissions = share, Pre-Permissions =share*/

      score = -ab(-beta,-alpha);
      backmove();
      /* Method Name = backmove
	  Vertex Name = plycnt, Post Permissions = share, Pre-Permissions =share// it shoudl be share
	  Vertex Name = moves, Post Permissions = pure, Pre-Permissions =pure
	  Vertex Name = height, Post Permissions = share, Pre-Permissions =share // height should be share
	  Vertex Name = columns, Post Permissions = share, Pre-Permissions =share
	  Vertex Name = rows, Post Permissions = share, Pre-Permissions =share
	  Vertex Name = dias, Post Permissions = share, Pre-Permissions =share*/
      return score;
    }
    if ((x = transpose()) != ABSENT) {//ABSENT = r
      score = x >> 5;
      if (score == DRAWLOSE) {//DRAWLOSE = r
        if ((beta = DRAW) <= alpha)//DRAW = r
          return score;
      } else if (score == DRAWWIN) { // DRAWWIN = r
        if ((alpha = DRAW) >= beta)
          return score;
      } else return score; // exact score
    }
    poscnt = posed; // posed = r
    l = besti = 0;	// initialize arbitrarily for silly javac
    score = Integer.MIN_VALUE;	// try to get the best bound if score > beta
    for (i = 0; i < nav; i++) {
      for (j = i, val = Integer.MIN_VALUE; j < nav; j++) {
        k = av[j];
        v = history[side][height[k]<<3|k];//history = r
        if (v > val) {
          val = v; l = j;
        }
      }
      j = av[l];
      if (i != l) {
        av[l] = av[i]; av[i] = j;
      }
      makemove(j);
      val = -ab(-beta,-alpha);
      backmove();
      if (val > score) {
        besti = i;
        if ((score = val) > alpha && (alpha = val) >= beta) {
          if (score == DRAW && i < nav-1)
            score = DRAWWIN;
          break;
        }
      }
    }
    if (besti > 0) {
      for (i = 0; i < besti; i++)
        history[side][height[av[i]]<<3|av[i]]--;	//history = rw// punish bad historiess
      history[side][height[av[besti]]<<3|av[besti]] += besti;
    }
    poscnt = posed - poscnt;
    for (work=1; (poscnt>>=1) != 0; work++) ;	// work=log #positions stored
    if (x != ABSENT) {
      if (score == -(x>>5))	// combine < and >
        score = DRAW;
      transrestore(score, work);
      /*Method Name = transrestore
	  Vertex Name = posed, Post Permissions = share, Pre-Permissions =share // its should be share
	  Vertex Name = htindex, Post Permissions = share, Pre-Permissions =share 
	  Vertex Name = PROBES, Post Permissions = immutable, Pre-Permissions =immutable
	  Vertex Name = ht, Post Permissions = share, Pre-Permissions =share
	  Vertex Name = lock, Post Permissions = share, Pre-Permissions =share
	  Vertex Name = hits, Post Permissions = pure, Pre-Permissions =pure
	  Vertex Name = he, Post Permissions = share, Pre-Permissions =share
	  Vertex Name = stride, Post Permissions = share, Pre-Permissions =share 
	  Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
	  Vertex Name = columns, Post Permissions = pure, Pre-Permissions =pure
	  Vertex Name = NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
	  Vertex Name = STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
	  Vertex Name = INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable*/
    } else transtore(score, work);
    /*Method Name = transtore
	Vertex Name = posed, Post Permissions = share, Pre-Permissions =share // it should be share
	Vertex Name = columns, Post Permissions = pure, Pre-Permissions =pure
	Vertex Name = lock, Post Permissions = share, Pre-Permissions =share
	Vertex Name = htindex, Post Permissions = share, Pre-Permissions =share
	Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
	Vertex Name = stride, Post Permissions = share, Pre-Permissions =share
	Vertex Name = NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
	Vertex Name = STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
	Vertex Name = INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
	Vertex Name = PROBES, Post Permissions = immutable, Pre-Permissions =immutable
	Vertex Name = he, Post Permissions = share, Pre-Permissions =share
	Vertex Name = hits, Post Permissions = share, Pre-Permissions =share // should be share
	Vertex Name = ht, Post Permissions = pure, Pre-Permissions =pure*/ // its should be share
    
    // if (plycnt == REPORTPLY) {
    //  System.out.println(toString() + "##-<=>+#".charAt(4+score) + work);
    //}
    return score;
  }

}
/*
Class Name = SearchGame
Class Name = JGFSearchBenchSizeA
Method Name = main
Vertex Name = history, Post Permissions = unique, Pre-Permissions =none
Vertex Name = TRANSIZE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = PROBES, Post Permissions = unique, Pre-Permissions =none
Vertex Name = REPORTPLY, Post Permissions = unique, Pre-Permissions =none
Vertex Name = UNK, Post Permissions = unique, Pre-Permissions =none
Vertex Name = LOSE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = DRAWLOSE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = DRAW, Post Permissions = unique, Pre-Permissions =none
Vertex Name = DRAWWIN, Post Permissions = unique, Pre-Permissions =none
Vertex Name = WIN, Post Permissions = unique, Pre-Permissions =none
Vertex Name = EMPTY, Post Permissions = unique, Pre-Permissions =none
Vertex Name = BLACK, Post Permissions = unique, Pre-Permissions =none
Vertex Name = WHITE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = EDGE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = startingMoves, Post Permissions = unique, Pre-Permissions =none
Vertex Name = colthr, Post Permissions = unique, Pre-Permissions =none
Vertex Name = colwon, Post Permissions = unique, Pre-Permissions =none
Vertex Name = NSAMELOCK, Post Permissions = unique, Pre-Permissions =none
Vertex Name = STRIDERANGE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = INTMODSTRIDERANGE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ABSENT, Post Permissions = unique, Pre-Permissions =none
Vertex Name = nodes, Post Permissions = unique, Pre-Permissions =none
Vertex Name = timers, Post Permissions = unique, Pre-Permissions =none
Vertex Name = name, Post Permissions = unique, Pre-Permissions =none
Vertex Name = opname, Post Permissions = unique, Pre-Permissions =none
Vertex Name = size, Post Permissions = unique, Pre-Permissions =none
Vertex Name = time, Post Permissions = unique, Pre-Permissions =none
Vertex Name = calls, Post Permissions = unique, Pre-Permissions =none
Vertex Name = opcount, Post Permissions = unique, Pre-Permissions =none
Vertex Name = on, Post Permissions = unique, Pre-Permissions =none
Vertex Name = plycnt, Post Permissions = unique, Pre-Permissions =none
Vertex Name = dias, Post Permissions = unique, Pre-Permissions =none
Vertex Name = columns, Post Permissions = unique, Pre-Permissions =none
Vertex Name = height, Post Permissions = unique, Pre-Permissions =none
Vertex Name = rows, Post Permissions = unique, Pre-Permissions =none
Vertex Name = moves, Post Permissions = unique, Pre-Permissions =none
Vertex Name = he, Post Permissions = unique, Pre-Permissions =none
Vertex Name = posed, Post Permissions = unique, Pre-Permissions =none
Vertex Name = hits, Post Permissions = unique, Pre-Permissions =none
Vertex Name = msecs, Post Permissions = unique, Pre-Permissions =none
Vertex Name = htindex, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ht, Post Permissions = unique, Pre-Permissions =none
Vertex Name = lock, Post Permissions = unique, Pre-Permissions =none
Vertex Name = stride, Post Permissions = unique, Pre-Permissions =none
Vertex Name = start_time, Post Permissions = unique, Pre-Permissions =none
Method Name = SearchGame
Vertex Name = ht, Post Permissions = unique, Pre-Permissions =none
Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = he, Post Permissions = unique, Pre-Permissions =none
Vertex Name = moves, Post Permissions = unique, Pre-Permissions =none
Vertex Name = rows, Post Permissions = unique, Pre-Permissions =none
Vertex Name = dias, Post Permissions = unique, Pre-Permissions =none
Vertex Name = columns, Post Permissions = unique, Pre-Permissions =none
Vertex Name = height, Post Permissions = unique, Pre-Permissions =none
Vertex Name = plycnt, Post Permissions = share, Pre-Permissions =share
Method Name = TransGame
Vertex Name = ht, Post Permissions = unique, Pre-Permissions =none
Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = he, Post Permissions = unique, Pre-Permissions =none
Vertex Name = moves, Post Permissions = unique, Pre-Permissions =none
Vertex Name = rows, Post Permissions = unique, Pre-Permissions =none
Vertex Name = dias, Post Permissions = unique, Pre-Permissions =none
Vertex Name = columns, Post Permissions = unique, Pre-Permissions =none
Vertex Name = height, Post Permissions = unique, Pre-Permissions =none
Vertex Name = plycnt, Post Permissions = share, Pre-Permissions =share
Method Name = Game
Vertex Name = moves, Post Permissions = unique, Pre-Permissions =none
Vertex Name = rows, Post Permissions = unique, Pre-Permissions =none
Vertex Name = dias, Post Permissions = unique, Pre-Permissions =none
Vertex Name = columns, Post Permissions = unique, Pre-Permissions =none
Vertex Name = height, Post Permissions = unique, Pre-Permissions =none
Vertex Name = plycnt, Post Permissions = share, Pre-Permissions =share
Method Name = reset
Vertex Name = plycnt, Post Permissions = share, Pre-Permissions =share
Vertex Name = dias, Post Permissions = share, Pre-Permissions =share
Vertex Name = columns, Post Permissions = share, Pre-Permissions =share
Vertex Name = height, Post Permissions = share, Pre-Permissions =share
Vertex Name = rows, Post Permissions = share, Pre-Permissions =share
Method Name = solve
Vertex Name = nodes, Post Permissions = share, Pre-Permissions =share
Vertex Name = msecs, Post Permissions = share, Pre-Permissions =share
Vertex Name = plycnt, Post Permissions = share, Pre-Permissions =share
Vertex Name = height, Post Permissions = share, Pre-Permissions =share
Vertex Name = colthr, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = columns, Post Permissions = share, Pre-Permissions =share
Vertex Name = WIN, Post Permissions = share, Pre-Permissions =share
Vertex Name = LOSE, Post Permissions = share, Pre-Permissions =share
Vertex Name = ABSENT, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = posed, Post Permissions = share, Pre-Permissions =share
Vertex Name = rows, Post Permissions = share, Pre-Permissions =share
Vertex Name = dias, Post Permissions = share, Pre-Permissions =share
Vertex Name = htindex, Post Permissions = share, Pre-Permissions =share
Vertex Name = PROBES, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ht, Post Permissions = share, Pre-Permissions =share
Vertex Name = lock, Post Permissions = share, Pre-Permissions =share
Vertex Name = he, Post Permissions = share, Pre-Permissions =share
Vertex Name = stride, Post Permissions = share, Pre-Permissions =share
Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = timers, Post Permissions = share, Pre-Permissions =share
Vertex Name = on, Post Permissions = share, Pre-Permissions =share
Vertex Name = name, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = start_time, Post Permissions = share, Pre-Permissions =share
Vertex Name = DRAW, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = DRAWLOSE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = DRAWWIN, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = history, Post Permissions = share, Pre-Permissions =share
Vertex Name = moves, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = hits, Post Permissions = share, Pre-Permissions =share
Vertex Name = time, Post Permissions = share, Pre-Permissions =share
Vertex Name = calls, Post Permissions = share, Pre-Permissions =share
Method Name = wins
Vertex Name = rows, Post Permissions = share, Pre-Permissions =share
Vertex Name = dias, Post Permissions = share, Pre-Permissions =share
Method Name = transpose
Vertex Name = htindex, Post Permissions = share, Pre-Permissions =share
Vertex Name = PROBES, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ht, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = lock, Post Permissions = share, Pre-Permissions =share
Vertex Name = he, Post Permissions = share, Pre-Permissions =share
Vertex Name = stride, Post Permissions = share, Pre-Permissions =share
Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ABSENT, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = columns, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = hash
Vertex Name = columns, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = lock, Post Permissions = share, Pre-Permissions =share
Vertex Name = htindex, Post Permissions = share, Pre-Permissions =share
Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = stride, Post Permissions = share, Pre-Permissions =share
Vertex Name = NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = startTimer
Vertex Name = timers, Post Permissions = share, Pre-Permissions =share
Vertex Name = on, Post Permissions = share, Pre-Permissions =share
Vertex Name = name, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = start_time, Post Permissions = share, Pre-Permissions =share
Method Name = start
Vertex Name = on, Post Permissions = share, Pre-Permissions =share
Vertex Name = name, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = start_time, Post Permissions = share, Pre-Permissions =share
Method Name = ab
Vertex Name = LOSE, Post Permissions = share, Pre-Permissions =share
Vertex Name = WIN, Post Permissions = share, Pre-Permissions =share
Vertex Name = nodes, Post Permissions = share, Pre-Permissions =share
Vertex Name = plycnt, Post Permissions = share, Pre-Permissions =share
Vertex Name = DRAW, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = height, Post Permissions = share, Pre-Permissions =share
Vertex Name = colthr, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = columns, Post Permissions = share, Pre-Permissions =share
Vertex Name = ABSENT, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = DRAWLOSE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = DRAWWIN, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = posed, Post Permissions = share, Pre-Permissions =share
Vertex Name = history, Post Permissions = share, Pre-Permissions =share
Vertex Name = rows, Post Permissions = share, Pre-Permissions =share
Vertex Name = dias, Post Permissions = share, Pre-Permissions =share
Vertex Name = moves, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = htindex, Post Permissions = share, Pre-Permissions =share
Vertex Name = PROBES, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ht, Post Permissions = share, Pre-Permissions =share
Vertex Name = lock, Post Permissions = share, Pre-Permissions =share
Vertex Name = he, Post Permissions = share, Pre-Permissions =share
Vertex Name = stride, Post Permissions = share, Pre-Permissions =share
Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = hits, Post Permissions = share, Pre-Permissions =share
Method Name = makemove
Vertex Name = moves, Post Permissions = share, Pre-Permissions =share
Vertex Name = plycnt, Post Permissions = share, Pre-Permissions =share
Vertex Name = height, Post Permissions = share, Pre-Permissions =share
Vertex Name = columns, Post Permissions = share, Pre-Permissions =share
Vertex Name = rows, Post Permissions = share, Pre-Permissions =share
Vertex Name = dias, Post Permissions = share, Pre-Permissions =share
Method Name = backmove
Vertex Name = plycnt, Post Permissions = share, Pre-Permissions =share
Vertex Name = moves, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = height, Post Permissions = share, Pre-Permissions =share
Vertex Name = columns, Post Permissions = share, Pre-Permissions =share
Vertex Name = rows, Post Permissions = share, Pre-Permissions =share
Vertex Name = dias, Post Permissions = share, Pre-Permissions =share
Method Name = transrestore
Vertex Name = posed, Post Permissions = share, Pre-Permissions =share
Vertex Name = htindex, Post Permissions = share, Pre-Permissions =share
Vertex Name = PROBES, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ht, Post Permissions = share, Pre-Permissions =share
Vertex Name = lock, Post Permissions = share, Pre-Permissions =share
Vertex Name = hits, Post Permissions = share, Pre-Permissions =share
Vertex Name = he, Post Permissions = share, Pre-Permissions =share
Vertex Name = stride, Post Permissions = share, Pre-Permissions =share
Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = columns, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = transput
Vertex Name = htindex, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = PROBES, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = he, Post Permissions = share, Pre-Permissions =share
Vertex Name = hits, Post Permissions = share, Pre-Permissions =share
Vertex Name = ht, Post Permissions = share, Pre-Permissions =share
Vertex Name = lock, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = stride, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
Method Name = transtore
Vertex Name = posed, Post Permissions = share, Pre-Permissions =share
Vertex Name = columns, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = lock, Post Permissions = share, Pre-Permissions =share
Vertex Name = htindex, Post Permissions = share, Pre-Permissions =share
Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = stride, Post Permissions = share, Pre-Permissions =share
Vertex Name = NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = PROBES, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = he, Post Permissions = share, Pre-Permissions =share
Vertex Name = hits, Post Permissions = share, Pre-Permissions =share
Vertex Name = ht, Post Permissions = pure, Pre-Permissions =pure
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
Class Name = ConnectFourConstants
Class Name = JGFSearchBenchSizeA
Method Name = main
Vertex Name = history, Post Permissions = unique, Pre-Permissions =none
Vertex Name = TRANSIZE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = PROBES, Post Permissions = unique, Pre-Permissions =none
Vertex Name = REPORTPLY, Post Permissions = unique, Pre-Permissions =none
Vertex Name = UNK, Post Permissions = unique, Pre-Permissions =none
Vertex Name = LOSE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = DRAWLOSE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = DRAW, Post Permissions = unique, Pre-Permissions =none
Vertex Name = DRAWWIN, Post Permissions = unique, Pre-Permissions =none
Vertex Name = WIN, Post Permissions = unique, Pre-Permissions =none
Vertex Name = EMPTY, Post Permissions = unique, Pre-Permissions =none
Vertex Name = BLACK, Post Permissions = unique, Pre-Permissions =none
Vertex Name = WHITE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = EDGE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = startingMoves, Post Permissions = unique, Pre-Permissions =none
Vertex Name = colthr, Post Permissions = unique, Pre-Permissions =none
Vertex Name = colwon, Post Permissions = unique, Pre-Permissions =none
Vertex Name = NSAMELOCK, Post Permissions = unique, Pre-Permissions =none
Vertex Name = STRIDERANGE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = INTMODSTRIDERANGE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ABSENT, Post Permissions = unique, Pre-Permissions =none
Vertex Name = nodes, Post Permissions = unique, Pre-Permissions =none
Vertex Name = timers, Post Permissions = unique, Pre-Permissions =none
Vertex Name = name, Post Permissions = unique, Pre-Permissions =none
Vertex Name = opname, Post Permissions = unique, Pre-Permissions =none
Vertex Name = size, Post Permissions = unique, Pre-Permissions =none
Vertex Name = time, Post Permissions = unique, Pre-Permissions =none
Vertex Name = calls, Post Permissions = unique, Pre-Permissions =none
Vertex Name = opcount, Post Permissions = unique, Pre-Permissions =none
Vertex Name = on, Post Permissions = unique, Pre-Permissions =none
Vertex Name = plycnt, Post Permissions = unique, Pre-Permissions =none
Vertex Name = dias, Post Permissions = unique, Pre-Permissions =none
Vertex Name = columns, Post Permissions = unique, Pre-Permissions =none
Vertex Name = height, Post Permissions = unique, Pre-Permissions =none
Vertex Name = rows, Post Permissions = unique, Pre-Permissions =none
Vertex Name = moves, Post Permissions = unique, Pre-Permissions =none
Vertex Name = he, Post Permissions = unique, Pre-Permissions =none
Vertex Name = posed, Post Permissions = unique, Pre-Permissions =none
Vertex Name = hits, Post Permissions = unique, Pre-Permissions =none
Vertex Name = msecs, Post Permissions = unique, Pre-Permissions =none
Vertex Name = htindex, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ht, Post Permissions = unique, Pre-Permissions =none
Vertex Name = lock, Post Permissions = unique, Pre-Permissions =none
Vertex Name = stride, Post Permissions = unique, Pre-Permissions =none
Vertex Name = start_time, Post Permissions = unique, Pre-Permissions =none
Class Name = Game
Class Name = JGFSearchBenchSizeA
Method Name = main
Vertex Name = history, Post Permissions = unique, Pre-Permissions =none
Vertex Name = TRANSIZE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = PROBES, Post Permissions = unique, Pre-Permissions =none
Vertex Name = REPORTPLY, Post Permissions = unique, Pre-Permissions =none
Vertex Name = UNK, Post Permissions = unique, Pre-Permissions =none
Vertex Name = LOSE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = DRAWLOSE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = DRAW, Post Permissions = unique, Pre-Permissions =none
Vertex Name = DRAWWIN, Post Permissions = unique, Pre-Permissions =none
Vertex Name = WIN, Post Permissions = unique, Pre-Permissions =none
Vertex Name = EMPTY, Post Permissions = unique, Pre-Permissions =none
Vertex Name = BLACK, Post Permissions = unique, Pre-Permissions =none
Vertex Name = WHITE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = EDGE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = startingMoves, Post Permissions = unique, Pre-Permissions =none
Vertex Name = colthr, Post Permissions = unique, Pre-Permissions =none
Vertex Name = colwon, Post Permissions = unique, Pre-Permissions =none
Vertex Name = NSAMELOCK, Post Permissions = unique, Pre-Permissions =none
Vertex Name = STRIDERANGE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = INTMODSTRIDERANGE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ABSENT, Post Permissions = unique, Pre-Permissions =none
Vertex Name = nodes, Post Permissions = unique, Pre-Permissions =none
Vertex Name = timers, Post Permissions = unique, Pre-Permissions =none
Vertex Name = name, Post Permissions = unique, Pre-Permissions =none
Vertex Name = opname, Post Permissions = unique, Pre-Permissions =none
Vertex Name = size, Post Permissions = unique, Pre-Permissions =none
Vertex Name = time, Post Permissions = unique, Pre-Permissions =none
Vertex Name = calls, Post Permissions = unique, Pre-Permissions =none
Vertex Name = opcount, Post Permissions = unique, Pre-Permissions =none
Vertex Name = on, Post Permissions = unique, Pre-Permissions =none
Vertex Name = plycnt, Post Permissions = unique, Pre-Permissions =none
Vertex Name = dias, Post Permissions = unique, Pre-Permissions =none
Vertex Name = columns, Post Permissions = unique, Pre-Permissions =none
Vertex Name = height, Post Permissions = unique, Pre-Permissions =none
Vertex Name = rows, Post Permissions = unique, Pre-Permissions =none
Vertex Name = moves, Post Permissions = unique, Pre-Permissions =none
Vertex Name = he, Post Permissions = unique, Pre-Permissions =none
Vertex Name = posed, Post Permissions = unique, Pre-Permissions =none
Vertex Name = hits, Post Permissions = unique, Pre-Permissions =none
Vertex Name = msecs, Post Permissions = unique, Pre-Permissions =none
Vertex Name = htindex, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ht, Post Permissions = unique, Pre-Permissions =none
Vertex Name = lock, Post Permissions = unique, Pre-Permissions =none
Vertex Name = stride, Post Permissions = unique, Pre-Permissions =none
Vertex Name = start_time, Post Permissions = unique, Pre-Permissions =none
Method Name = Game
Vertex Name = moves, Post Permissions = unique, Pre-Permissions =none
Vertex Name = rows, Post Permissions = unique, Pre-Permissions =none
Vertex Name = dias, Post Permissions = unique, Pre-Permissions =none
Vertex Name = columns, Post Permissions = unique, Pre-Permissions =none
Vertex Name = height, Post Permissions = unique, Pre-Permissions =none
Vertex Name = plycnt, Post Permissions = share, Pre-Permissions =share
Method Name = reset
Vertex Name = plycnt, Post Permissions = share, Pre-Permissions =share
Vertex Name = dias, Post Permissions = share, Pre-Permissions =share
Vertex Name = columns, Post Permissions = share, Pre-Permissions =share
Vertex Name = height, Post Permissions = share, Pre-Permissions =share
Vertex Name = rows, Post Permissions = share, Pre-Permissions =share
Method Name = toString
Vertex Name = plycnt, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = moves, Post Permissions = pure, Pre-Permissions =pure
Method Name = wins
Vertex Name = rows, Post Permissions = share, Pre-Permissions =share
Vertex Name = dias, Post Permissions = share, Pre-Permissions =share
Method Name = backmove
Vertex Name = plycnt, Post Permissions = share, Pre-Permissions =share
Vertex Name = moves, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = height, Post Permissions = share, Pre-Permissions =share
Vertex Name = columns, Post Permissions = share, Pre-Permissions =share
Vertex Name = rows, Post Permissions = share, Pre-Permissions =share
Vertex Name = dias, Post Permissions = share, Pre-Permissions =share
Method Name = makemove
Vertex Name = moves, Post Permissions = share, Pre-Permissions =share
Vertex Name = plycnt, Post Permissions = share, Pre-Permissions =share
Vertex Name = height, Post Permissions = share, Pre-Permissions =share
Vertex Name = columns, Post Permissions = share, Pre-Permissions =share
Vertex Name = rows, Post Permissions = share, Pre-Permissions =share
Vertex Name = dias, Post Permissions = share, Pre-Permissions =share
Class Name = TransGame
Class Name = JGFSearchBenchSizeA
Method Name = main
Vertex Name = history, Post Permissions = unique, Pre-Permissions =none
Vertex Name = TRANSIZE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = PROBES, Post Permissions = unique, Pre-Permissions =none
Vertex Name = REPORTPLY, Post Permissions = unique, Pre-Permissions =none
Vertex Name = UNK, Post Permissions = unique, Pre-Permissions =none
Vertex Name = LOSE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = DRAWLOSE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = DRAW, Post Permissions = unique, Pre-Permissions =none
Vertex Name = DRAWWIN, Post Permissions = unique, Pre-Permissions =none
Vertex Name = WIN, Post Permissions = unique, Pre-Permissions =none
Vertex Name = EMPTY, Post Permissions = unique, Pre-Permissions =none
Vertex Name = BLACK, Post Permissions = unique, Pre-Permissions =none
Vertex Name = WHITE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = EDGE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = startingMoves, Post Permissions = unique, Pre-Permissions =none
Vertex Name = colthr, Post Permissions = unique, Pre-Permissions =none
Vertex Name = colwon, Post Permissions = unique, Pre-Permissions =none
Vertex Name = NSAMELOCK, Post Permissions = unique, Pre-Permissions =none
Vertex Name = STRIDERANGE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = INTMODSTRIDERANGE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ABSENT, Post Permissions = unique, Pre-Permissions =none
Vertex Name = nodes, Post Permissions = unique, Pre-Permissions =none
Vertex Name = timers, Post Permissions = unique, Pre-Permissions =none
Vertex Name = name, Post Permissions = unique, Pre-Permissions =none
Vertex Name = opname, Post Permissions = unique, Pre-Permissions =none
Vertex Name = size, Post Permissions = unique, Pre-Permissions =none
Vertex Name = time, Post Permissions = unique, Pre-Permissions =none
Vertex Name = calls, Post Permissions = unique, Pre-Permissions =none
Vertex Name = opcount, Post Permissions = unique, Pre-Permissions =none
Vertex Name = on, Post Permissions = unique, Pre-Permissions =none
Vertex Name = plycnt, Post Permissions = unique, Pre-Permissions =none
Vertex Name = dias, Post Permissions = unique, Pre-Permissions =none
Vertex Name = columns, Post Permissions = unique, Pre-Permissions =none
Vertex Name = height, Post Permissions = unique, Pre-Permissions =none
Vertex Name = rows, Post Permissions = unique, Pre-Permissions =none
Vertex Name = moves, Post Permissions = unique, Pre-Permissions =none
Vertex Name = he, Post Permissions = unique, Pre-Permissions =none
Vertex Name = posed, Post Permissions = unique, Pre-Permissions =none
Vertex Name = hits, Post Permissions = unique, Pre-Permissions =none
Vertex Name = msecs, Post Permissions = unique, Pre-Permissions =none
Vertex Name = htindex, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ht, Post Permissions = unique, Pre-Permissions =none
Vertex Name = lock, Post Permissions = unique, Pre-Permissions =none
Vertex Name = stride, Post Permissions = unique, Pre-Permissions =none
Vertex Name = start_time, Post Permissions = unique, Pre-Permissions =none
Method Name = TransGame
Vertex Name = ht, Post Permissions = unique, Pre-Permissions =none
Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = he, Post Permissions = unique, Pre-Permissions =none
Vertex Name = moves, Post Permissions = unique, Pre-Permissions =none
Vertex Name = rows, Post Permissions = unique, Pre-Permissions =none
Vertex Name = dias, Post Permissions = unique, Pre-Permissions =none
Vertex Name = columns, Post Permissions = unique, Pre-Permissions =none
Vertex Name = height, Post Permissions = unique, Pre-Permissions =none
Vertex Name = plycnt, Post Permissions = share, Pre-Permissions =share
Method Name = emptyTT
Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = he, Post Permissions = share, Pre-Permissions =share
Vertex Name = posed, Post Permissions = share, Pre-Permissions =share
Vertex Name = hits, Post Permissions = share, Pre-Permissions =share
Method Name = hitRate
Vertex Name = posed, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = hits, Post Permissions = pure, Pre-Permissions =pure
Method Name = hash
Vertex Name = columns, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = lock, Post Permissions = share, Pre-Permissions =share
Vertex Name = htindex, Post Permissions = share, Pre-Permissions =share
Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = stride, Post Permissions = share, Pre-Permissions =share
Vertex Name = NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = transpose
Vertex Name = htindex, Post Permissions = share, Pre-Permissions =share
Vertex Name = PROBES, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ht, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = lock, Post Permissions = share, Pre-Permissions =share
Vertex Name = he, Post Permissions = share, Pre-Permissions =share
Vertex Name = stride, Post Permissions = share, Pre-Permissions =share
Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ABSENT, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = columns, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = result
Vertex Name = ABSENT, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = htindex, Post Permissions = share, Pre-Permissions =share
Vertex Name = PROBES, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ht, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = lock, Post Permissions = share, Pre-Permissions =share
Vertex Name = he, Post Permissions = share, Pre-Permissions =share
Vertex Name = stride, Post Permissions = share, Pre-Permissions =share
Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = columns, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = transrestore
Vertex Name = posed, Post Permissions = share, Pre-Permissions =share
Vertex Name = htindex, Post Permissions = share, Pre-Permissions =share
Vertex Name = PROBES, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ht, Post Permissions = share, Pre-Permissions =share
Vertex Name = lock, Post Permissions = share, Pre-Permissions =share
Vertex Name = hits, Post Permissions = share, Pre-Permissions =share
Vertex Name = he, Post Permissions = share, Pre-Permissions =share
Vertex Name = stride, Post Permissions = share, Pre-Permissions =share
Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = columns, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = transtore
Vertex Name = posed, Post Permissions = share, Pre-Permissions =share
Vertex Name = columns, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = lock, Post Permissions = share, Pre-Permissions =share
Vertex Name = htindex, Post Permissions = share, Pre-Permissions =share
Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = stride, Post Permissions = share, Pre-Permissions =share
Vertex Name = NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = PROBES, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = he, Post Permissions = share, Pre-Permissions =share
Vertex Name = hits, Post Permissions = share, Pre-Permissions =share
Vertex Name = ht, Post Permissions = pure, Pre-Permissions =pure
Method Name = transput
Vertex Name = htindex, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = PROBES, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = he, Post Permissions = share, Pre-Permissions =share
Vertex Name = hits, Post Permissions = share, Pre-Permissions =share
Vertex Name = ht, Post Permissions = share, Pre-Permissions =share
Vertex Name = lock, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = stride, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
Method Name = htstat
Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = he, Post Permissions = share, Pre-Permissions =share
Vertex Name = LOSE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = DRAWLOSE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = DRAW, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = DRAWWIN, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = WIN, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = posed, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = hits, Post Permissions = pure, Pre-Permissions =pure
Class Name = JGFSearchBench
Class Name = JGFSearchBenchSizeA
Method Name = main
Vertex Name = history, Post Permissions = unique, Pre-Permissions =none
Vertex Name = TRANSIZE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = PROBES, Post Permissions = unique, Pre-Permissions =none
Vertex Name = REPORTPLY, Post Permissions = unique, Pre-Permissions =none
Vertex Name = UNK, Post Permissions = unique, Pre-Permissions =none
Vertex Name = LOSE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = DRAWLOSE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = DRAW, Post Permissions = unique, Pre-Permissions =none
Vertex Name = DRAWWIN, Post Permissions = unique, Pre-Permissions =none
Vertex Name = WIN, Post Permissions = unique, Pre-Permissions =none
Vertex Name = EMPTY, Post Permissions = unique, Pre-Permissions =none
Vertex Name = BLACK, Post Permissions = unique, Pre-Permissions =none
Vertex Name = WHITE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = EDGE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = startingMoves, Post Permissions = unique, Pre-Permissions =none
Vertex Name = colthr, Post Permissions = unique, Pre-Permissions =none
Vertex Name = colwon, Post Permissions = unique, Pre-Permissions =none
Vertex Name = NSAMELOCK, Post Permissions = unique, Pre-Permissions =none
Vertex Name = STRIDERANGE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = INTMODSTRIDERANGE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ABSENT, Post Permissions = unique, Pre-Permissions =none
Vertex Name = nodes, Post Permissions = unique, Pre-Permissions =none
Vertex Name = timers, Post Permissions = unique, Pre-Permissions =none
Vertex Name = name, Post Permissions = unique, Pre-Permissions =none
Vertex Name = opname, Post Permissions = unique, Pre-Permissions =none
Vertex Name = size, Post Permissions = unique, Pre-Permissions =none
Vertex Name = time, Post Permissions = unique, Pre-Permissions =none
Vertex Name = calls, Post Permissions = unique, Pre-Permissions =none
Vertex Name = opcount, Post Permissions = unique, Pre-Permissions =none
Vertex Name = on, Post Permissions = unique, Pre-Permissions =none
Vertex Name = plycnt, Post Permissions = unique, Pre-Permissions =none
Vertex Name = dias, Post Permissions = unique, Pre-Permissions =none
Vertex Name = columns, Post Permissions = unique, Pre-Permissions =none
Vertex Name = height, Post Permissions = unique, Pre-Permissions =none
Vertex Name = rows, Post Permissions = unique, Pre-Permissions =none
Vertex Name = moves, Post Permissions = unique, Pre-Permissions =none
Vertex Name = he, Post Permissions = unique, Pre-Permissions =none
Vertex Name = posed, Post Permissions = unique, Pre-Permissions =none
Vertex Name = hits, Post Permissions = unique, Pre-Permissions =none
Vertex Name = msecs, Post Permissions = unique, Pre-Permissions =none
Vertex Name = htindex, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ht, Post Permissions = unique, Pre-Permissions =none
Vertex Name = lock, Post Permissions = unique, Pre-Permissions =none
Vertex Name = stride, Post Permissions = unique, Pre-Permissions =none
Vertex Name = start_time, Post Permissions = unique, Pre-Permissions =none
Method Name = JGFsetsize
Vertex Name = size, Post Permissions = share, Pre-Permissions =share
Method Name = JGFinitialise
Vertex Name = startingMoves, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = size, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = plycnt, Post Permissions = share, Pre-Permissions =share
Vertex Name = dias, Post Permissions = share, Pre-Permissions =share
Vertex Name = columns, Post Permissions = share, Pre-Permissions =share
Vertex Name = height, Post Permissions = share, Pre-Permissions =share
Vertex Name = rows, Post Permissions = share, Pre-Permissions =share
Vertex Name = moves, Post Permissions = share, Pre-Permissions =share
Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = he, Post Permissions = share, Pre-Permissions =share
Vertex Name = posed, Post Permissions = share, Pre-Permissions =share
Vertex Name = hits, Post Permissions = share, Pre-Permissions =share
Method Name = JGFapplication
Vertex Name = nodes, Post Permissions = share, Pre-Permissions =share
Vertex Name = msecs, Post Permissions = share, Pre-Permissions =share
Vertex Name = plycnt, Post Permissions = share, Pre-Permissions =share
Vertex Name = height, Post Permissions = share, Pre-Permissions =share
Vertex Name = colthr, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = columns, Post Permissions = share, Pre-Permissions =share
Vertex Name = WIN, Post Permissions = share, Pre-Permissions =share
Vertex Name = LOSE, Post Permissions = share, Pre-Permissions =share
Vertex Name = ABSENT, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = posed, Post Permissions = share, Pre-Permissions =share
Vertex Name = rows, Post Permissions = share, Pre-Permissions =share
Vertex Name = dias, Post Permissions = share, Pre-Permissions =share
Vertex Name = htindex, Post Permissions = share, Pre-Permissions =share
Vertex Name = PROBES, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ht, Post Permissions = share, Pre-Permissions =share
Vertex Name = lock, Post Permissions = share, Pre-Permissions =share
Vertex Name = he, Post Permissions = share, Pre-Permissions =share
Vertex Name = stride, Post Permissions = share, Pre-Permissions =share
Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = timers, Post Permissions = share, Pre-Permissions =share
Vertex Name = on, Post Permissions = share, Pre-Permissions =share
Vertex Name = name, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = start_time, Post Permissions = share, Pre-Permissions =share
Vertex Name = DRAW, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = DRAWLOSE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = DRAWWIN, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = history, Post Permissions = share, Pre-Permissions =share
Vertex Name = moves, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = hits, Post Permissions = share, Pre-Permissions =share
Vertex Name = time, Post Permissions = share, Pre-Permissions =share
Vertex Name = calls, Post Permissions = share, Pre-Permissions =share
Method Name = JGFvalidate
Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = he, Post Permissions = share, Pre-Permissions =share
Vertex Name = size, Post Permissions = pure, Pre-Permissions =pure
Method Name = JGFtidyup
Vertex Name = ht, Post Permissions = unique, Pre-Permissions =none
Vertex Name = he, Post Permissions = unique, Pre-Permissions =none
Method Name = JGFrun
Vertex Name = nodes, Post Permissions = share, Pre-Permissions =share
Vertex Name = timers, Post Permissions = share, Pre-Permissions =share
Vertex Name = name, Post Permissions = share, Pre-Permissions =share
Vertex Name = opname, Post Permissions = share, Pre-Permissions =share
Vertex Name = size, Post Permissions = share, Pre-Permissions =share
Vertex Name = time, Post Permissions = share, Pre-Permissions =share
Vertex Name = calls, Post Permissions = share, Pre-Permissions =share
Vertex Name = opcount, Post Permissions = share, Pre-Permissions =share
Vertex Name = on, Post Permissions = share, Pre-Permissions =share
Vertex Name = startingMoves, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = plycnt, Post Permissions = share, Pre-Permissions =share
Vertex Name = dias, Post Permissions = share, Pre-Permissions =share
Vertex Name = columns, Post Permissions = share, Pre-Permissions =share
Vertex Name = height, Post Permissions = share, Pre-Permissions =share
Vertex Name = rows, Post Permissions = share, Pre-Permissions =share
Vertex Name = moves, Post Permissions = share, Pre-Permissions =share
Vertex Name = TRANSIZE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = he, Post Permissions = unique, Pre-Permissions =none
Vertex Name = posed, Post Permissions = share, Pre-Permissions =share
Vertex Name = hits, Post Permissions = share, Pre-Permissions =share
Vertex Name = msecs, Post Permissions = share, Pre-Permissions =share
Vertex Name = colthr, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = WIN, Post Permissions = share, Pre-Permissions =share
Vertex Name = LOSE, Post Permissions = share, Pre-Permissions =share
Vertex Name = ABSENT, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = htindex, Post Permissions = share, Pre-Permissions =share
Vertex Name = PROBES, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ht, Post Permissions = unique, Pre-Permissions =none
Vertex Name = lock, Post Permissions = share, Pre-Permissions =share
Vertex Name = stride, Post Permissions = share, Pre-Permissions =share
Vertex Name = NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = start_time, Post Permissions = share, Pre-Permissions =share
Vertex Name = DRAW, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = DRAWLOSE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = DRAWWIN, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = history, Post Permissions = share, Pre-Permissions =share
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
Class Name = JGFSearchBenchSizeA
Class Name = JGFSearchBenchSizeA
Method Name = main
Vertex Name = history, Post Permissions = unique, Pre-Permissions =none
Vertex Name = TRANSIZE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = PROBES, Post Permissions = unique, Pre-Permissions =none
Vertex Name = REPORTPLY, Post Permissions = unique, Pre-Permissions =none
Vertex Name = UNK, Post Permissions = unique, Pre-Permissions =none
Vertex Name = LOSE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = DRAWLOSE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = DRAW, Post Permissions = unique, Pre-Permissions =none
Vertex Name = DRAWWIN, Post Permissions = unique, Pre-Permissions =none
Vertex Name = WIN, Post Permissions = unique, Pre-Permissions =none
Vertex Name = EMPTY, Post Permissions = unique, Pre-Permissions =none
Vertex Name = BLACK, Post Permissions = unique, Pre-Permissions =none
Vertex Name = WHITE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = EDGE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = startingMoves, Post Permissions = unique, Pre-Permissions =none
Vertex Name = colthr, Post Permissions = unique, Pre-Permissions =none
Vertex Name = colwon, Post Permissions = unique, Pre-Permissions =none
Vertex Name = NSAMELOCK, Post Permissions = unique, Pre-Permissions =none
Vertex Name = STRIDERANGE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = INTMODSTRIDERANGE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ABSENT, Post Permissions = unique, Pre-Permissions =none
Vertex Name = nodes, Post Permissions = unique, Pre-Permissions =none
Vertex Name = timers, Post Permissions = unique, Pre-Permissions =none
Vertex Name = name, Post Permissions = unique, Pre-Permissions =none
Vertex Name = opname, Post Permissions = unique, Pre-Permissions =none
Vertex Name = size, Post Permissions = unique, Pre-Permissions =none
Vertex Name = time, Post Permissions = unique, Pre-Permissions =none
Vertex Name = calls, Post Permissions = unique, Pre-Permissions =none
Vertex Name = opcount, Post Permissions = unique, Pre-Permissions =none
Vertex Name = on, Post Permissions = unique, Pre-Permissions =none
Vertex Name = plycnt, Post Permissions = unique, Pre-Permissions =none
Vertex Name = dias, Post Permissions = unique, Pre-Permissions =none
Vertex Name = columns, Post Permissions = unique, Pre-Permissions =none
Vertex Name = height, Post Permissions = unique, Pre-Permissions =none
Vertex Name = rows, Post Permissions = unique, Pre-Permissions =none
Vertex Name = moves, Post Permissions = unique, Pre-Permissions =none
Vertex Name = he, Post Permissions = unique, Pre-Permissions =none
Vertex Name = posed, Post Permissions = unique, Pre-Permissions =none
Vertex Name = hits, Post Permissions = unique, Pre-Permissions =none
Vertex Name = msecs, Post Permissions = unique, Pre-Permissions =none
Vertex Name = htindex, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ht, Post Permissions = unique, Pre-Permissions =none
Vertex Name = lock, Post Permissions = unique, Pre-Permissions =none
Vertex Name = stride, Post Permissions = unique, Pre-Permissions =none
Vertex Name = start_time, Post Permissions = unique, Pre-Permissions =none
Method Name = printHeader
*/
/*
Class Name = SearchGame
Class Name = JGFSearchBenchSizeA
Method Name = SearchGame
Vertex Name = TransGame.ht, Post Permissions = unique, Pre-Permissions =none
Vertex Name = TransGame.TRANSIZE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = TransGame.he, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.moves, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.rows, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.dias, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.columns, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.height, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.plycnt, Post Permissions = share, Pre-Permissions =share
Method Name = TransGame
Vertex Name = TransGame.ht, Post Permissions = unique, Pre-Permissions =none
Vertex Name = TransGame.TRANSIZE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = TransGame.he, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.moves, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.rows, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.dias, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.columns, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.height, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.plycnt, Post Permissions = share, Pre-Permissions =share
Method Name = Game
Vertex Name = Game.moves, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.rows, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.dias, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.columns, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.height, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.plycnt, Post Permissions = share, Pre-Permissions =share
Method Name = reset
Vertex Name = Game.plycnt, Post Permissions = share, Pre-Permissions =share
Vertex Name = Game.dias, Post Permissions = share, Pre-Permissions =share
Vertex Name = Game.columns, Post Permissions = share, Pre-Permissions =share
Vertex Name = Game.height, Post Permissions = share, Pre-Permissions =share
Vertex Name = Game.rows, Post Permissions = share, Pre-Permissions =share
Method Name = solve
Vertex Name = sb.nodes, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.msecs, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.plycnt, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.height, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.colthr, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.columns, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.WIN, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.LOSE, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.ABSENT, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.posed, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.rows, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.dias, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.htindex, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.PROBES, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.ht, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.lock, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.he, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.stride, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.TRANSIZE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = JGFInstrumentor.timers, Post Permissions = share, Pre-Permissions =share
Vertex Name = JGFTimer.on, Post Permissions = share, Pre-Permissions =share
Vertex Name = JGFTimer.name, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = JGFTimer.start_time, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.DRAW, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.DRAWLOSE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.DRAWWIN, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.history, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.moves, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = sb.hits, Post Permissions = share, Pre-Permissions =share
Vertex Name = JGFTimer.time, Post Permissions = share, Pre-Permissions =share
Vertex Name = JGFTimer.calls, Post Permissions = share, Pre-Permissions =share
Method Name = wins
Vertex Name = sb.rows, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.dias, Post Permissions = share, Pre-Permissions =share
Method Name = transpose
Vertex Name = sb.htindex, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.PROBES, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.ht, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = sb.lock, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.he, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.stride, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.TRANSIZE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.ABSENT, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.columns, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = sb.NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = hash
Vertex Name = sb.columns, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = sb.lock, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.htindex, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.TRANSIZE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.stride, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = startTimer
Vertex Name = JGFInstrumentor.timers, Post Permissions = share, Pre-Permissions =share
Vertex Name = JGFTimer.on, Post Permissions = share, Pre-Permissions =share
Vertex Name = JGFTimer.name, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = JGFTimer.start_time, Post Permissions = share, Pre-Permissions =share
Method Name = start
Vertex Name = JGFTimer.on, Post Permissions = share, Pre-Permissions =share
Vertex Name = JGFTimer.name, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = JGFTimer.start_time, Post Permissions = share, Pre-Permissions =share
Method Name = ab
Vertex Name = sb.LOSE, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.WIN, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.nodes, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.plycnt, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.DRAW, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.height, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.colthr, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.columns, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.ABSENT, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.DRAWLOSE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.DRAWWIN, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.posed, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.history, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.rows, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.dias, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.moves, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = sb.htindex, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.PROBES, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.ht, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.lock, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.he, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.stride, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.TRANSIZE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.hits, Post Permissions = share, Pre-Permissions =share
Method Name = makemove
Vertex Name = sb.moves, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.plycnt, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.height, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.columns, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.rows, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.dias, Post Permissions = share, Pre-Permissions =share
Method Name = backmove
Vertex Name = sb.plycnt, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.moves, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = sb.height, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.columns, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.rows, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.dias, Post Permissions = share, Pre-Permissions =share
Method Name = transrestore
Vertex Name = sb.posed, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.htindex, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.PROBES, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.ht, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.lock, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.hits, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.he, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.stride, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.TRANSIZE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.columns, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = sb.NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = transput
Vertex Name = sb.htindex, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = sb.PROBES, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.he, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.hits, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.ht, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.lock, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = sb.stride, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = sb.TRANSIZE, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = transtore
Vertex Name = sb.posed, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.columns, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = sb.lock, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.htindex, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.TRANSIZE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.stride, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.PROBES, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.he, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.hits, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.ht, Post Permissions = pure, Pre-Permissions =pure
Method Name = stopTimer
Vertex Name = JGFInstrumentor.timers, Post Permissions = share, Pre-Permissions =share
Vertex Name = JGFTimer.time, Post Permissions = share, Pre-Permissions =share
Vertex Name = JGFTimer.start_time, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = JGFTimer.on, Post Permissions = share, Pre-Permissions =share
Vertex Name = JGFTimer.name, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = JGFTimer.calls, Post Permissions = share, Pre-Permissions =share
Method Name = stop
Vertex Name = JGFTimer.time, Post Permissions = share, Pre-Permissions =share
Vertex Name = JGFTimer.start_time, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = JGFTimer.on, Post Permissions = share, Pre-Permissions =share
Vertex Name = JGFTimer.name, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = JGFTimer.calls, Post Permissions = share, Pre-Permissions =share
Class Name = ConnectFourConstants
Class Name = JGFSearchBenchSizeA
Method Name = main
Vertex Name = SearchGame.history, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ConnectFourConstants.TRANSIZE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ConnectFourConstants.PROBES, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ConnectFourConstants.REPORTPLY, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ConnectFourConstants.UNK, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ConnectFourConstants.LOSE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ConnectFourConstants.DRAWLOSE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ConnectFourConstants.DRAW, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ConnectFourConstants.DRAWWIN, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ConnectFourConstants.WIN, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ConnectFourConstants.EMPTY, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ConnectFourConstants.BLACK, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ConnectFourConstants.WHITE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ConnectFourConstants.EDGE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = ConnectFourConstants.startingMoves, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.colthr, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.colwon, Post Permissions = unique, Pre-Permissions =none
Vertex Name = TransGame.NSAMELOCK, Post Permissions = unique, Pre-Permissions =none
Vertex Name = TransGame.STRIDERANGE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = TransGame.INTMODSTRIDERANGE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = TransGame.ABSENT, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sb.nodes, Post Permissions = unique, Pre-Permissions =none
Vertex Name = JGFInstrumentor.timers, Post Permissions = unique, Pre-Permissions =none
Vertex Name = JGFTimer.name, Post Permissions = unique, Pre-Permissions =none
Vertex Name = JGFTimer.opname, Post Permissions = unique, Pre-Permissions =none
Vertex Name = JGFTimer.size, Post Permissions = unique, Pre-Permissions =none
Vertex Name = JGFTimer.time, Post Permissions = unique, Pre-Permissions =none
Vertex Name = JGFTimer.calls, Post Permissions = unique, Pre-Permissions =none
Vertex Name = JGFTimer.opcount, Post Permissions = unique, Pre-Permissions =none
Vertex Name = JGFTimer.on, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sb.size, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sb.startingMoves, Post Permissions = none, Pre-Permissions =none
Vertex Name = Game.plycnt, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.dias, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.columns, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.height, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.rows, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sb.moves, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sb.plycnt, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sb.height, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sb.columns, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sb.rows, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sb.dias, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sb.TRANSIZE, Post Permissions = none, Pre-Permissions =none
Vertex Name = sb.he, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sb.posed, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sb.hits, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sb.msecs, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sb.colthr, Post Permissions = none, Pre-Permissions =none
Vertex Name = sb.WIN, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sb.LOSE, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sb.ABSENT, Post Permissions = none, Pre-Permissions =none
Vertex Name = sb.htindex, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sb.PROBES, Post Permissions = none, Pre-Permissions =none
Vertex Name = sb.ht, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sb.lock, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sb.stride, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sb.NSAMELOCK, Post Permissions = none, Pre-Permissions =none
Vertex Name = sb.STRIDERANGE, Post Permissions = none, Pre-Permissions =none
Vertex Name = sb.INTMODSTRIDERANGE, Post Permissions = none, Pre-Permissions =none
Vertex Name = JGFTimer.start_time, Post Permissions = unique, Pre-Permissions =none
Vertex Name = sb.DRAW, Post Permissions = none, Pre-Permissions =none
Vertex Name = sb.DRAWLOSE, Post Permissions = none, Pre-Permissions =none
Vertex Name = sb.DRAWWIN, Post Permissions = none, Pre-Permissions =none
Vertex Name = sb.history, Post Permissions = unique, Pre-Permissions =none
Class Name = TransGame
Method Name = TransGame
Vertex Name = TransGame.ht, Post Permissions = unique, Pre-Permissions =none
Vertex Name = TransGame.TRANSIZE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = TransGame.he, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.moves, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.rows, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.dias, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.columns, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.height, Post Permissions = unique, Pre-Permissions =none
Vertex Name = Game.plycnt, Post Permissions = share, Pre-Permissions =share
Method Name = emptyTT
Vertex Name = sb.TRANSIZE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.he, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.posed, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.hits, Post Permissions = share, Pre-Permissions =share
Method Name = hitRate
Vertex Name = TransGame.posed, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = TransGame.hits, Post Permissions = pure, Pre-Permissions =pure
Method Name = hash
Vertex Name = sb.columns, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = sb.lock, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.htindex, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.TRANSIZE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.stride, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = transpose
Vertex Name = sb.htindex, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.PROBES, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.ht, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = sb.lock, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.he, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.stride, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.TRANSIZE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.ABSENT, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.columns, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = sb.NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = result
Vertex Name = TransGame.ABSENT, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.htindex, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.PROBES, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.ht, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = sb.lock, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.he, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.stride, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.TRANSIZE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.ABSENT, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.columns, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = sb.NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = transrestore
Vertex Name = sb.posed, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.htindex, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.PROBES, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.ht, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.lock, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.hits, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.he, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.stride, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.TRANSIZE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.columns, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = sb.NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = transtore
Vertex Name = sb.posed, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.columns, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = sb.lock, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.htindex, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.TRANSIZE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.stride, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.NSAMELOCK, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.STRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.INTMODSTRIDERANGE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.PROBES, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.he, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.hits, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.ht, Post Permissions = pure, Pre-Permissions =pure
Method Name = transput
Vertex Name = sb.htindex, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = sb.PROBES, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = sb.he, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.hits, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.ht, Post Permissions = share, Pre-Permissions =share
Vertex Name = sb.lock, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = sb.stride, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = sb.TRANSIZE, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = htstat
Vertex Name = ConnectFourConstants.TRANSIZE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = ConnectFourConstants.he, Post Permissions = share, Pre-Permissions =share
Vertex Name = ConnectFourConstants.LOSE, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = ConnectFourConstants.DRAWLOSE, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = ConnectFourConstants.DRAW, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = ConnectFourConstants.DRAWWIN, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = ConnectFourConstants.WIN, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = TransGame.posed, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = TransGame.hits, Post Permissions = pure, Pre-Permissions =pure

Class Name = SearchGame
Method Name = main
Ref-Var= search.TransGame.history, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.TRANSIZE, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.PROBES, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.REPORTPLY, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.UNK, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.LOSE, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.DRAWLOSE, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.DRAW, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.DRAWWIN, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.WIN, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.EMPTY, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.BLACK, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.WHITE, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.EDGE, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.startingMoves, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.colthr, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.colwon, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.NSAMELOCK, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.STRIDERANGE, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.INTMODSTRIDERANGE, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.ABSENT, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.size, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.startingMoves, Pre-Permissions=none, Post Permissions=none
Ref-Var= search.Game.plycnt, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.dias, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.columns, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.height, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.rows, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.moves, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.plycnt, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.height, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.columns, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.rows, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.dias, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.TRANSIZE, Pre-Permissions=none, Post Permissions=none
Ref-Var= sb.he, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.posed, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.hits, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.nodes, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.msecs, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.colthr, Pre-Permissions=none, Post Permissions=none
Ref-Var= sb.WIN, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.LOSE, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.ABSENT, Pre-Permissions=none, Post Permissions=none
Ref-Var= sb.htindex, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.PROBES, Pre-Permissions=none, Post Permissions=none
Ref-Var= sb.ht, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.lock, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.stride, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.NSAMELOCK, Pre-Permissions=none, Post Permissions=none
Ref-Var= sb.STRIDERANGE, Pre-Permissions=none, Post Permissions=none
Ref-Var= sb.INTMODSTRIDERANGE, Pre-Permissions=none, Post Permissions=none
Ref-Var= sb.DRAW, Pre-Permissions=none, Post Permissions=none
Ref-Var= sb.DRAWLOSE, Pre-Permissions=none, Post Permissions=none
Ref-Var= sb.DRAWWIN, Pre-Permissions=none, Post Permissions=none
Ref-Var= sb.history, Pre-Permissions=none, Post Permissions=unique
Method Name = SearchGame
Ref-Var= search.TransGame.ht, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.TransGame.TRANSIZE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= search.TransGame.he, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.moves, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.rows, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.dias, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.columns, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.height, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.plycnt, Pre-Permissions=share, Post Permissions=share
Method Name = TransGame
Ref-Var= search.TransGame.ht, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.TransGame.TRANSIZE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= search.TransGame.he, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.moves, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.rows, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.dias, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.columns, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.height, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.plycnt, Pre-Permissions=share, Post Permissions=share
Method Name = Game
Ref-Var= search.Game.moves, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.rows, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.dias, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.columns, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.height, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.plycnt, Pre-Permissions=share, Post Permissions=share
Method Name = reset
Ref-Var= search.Game.plycnt, Pre-Permissions=share, Post Permissions=share
Ref-Var= search.Game.dias, Pre-Permissions=share, Post Permissions=share
Ref-Var= search.Game.columns, Pre-Permissions=share, Post Permissions=share
Ref-Var= search.Game.height, Pre-Permissions=share, Post Permissions=share
Ref-Var= search.Game.rows, Pre-Permissions=share, Post Permissions=share
Method Name = solve
Ref-Var= sb.nodes, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.msecs, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.plycnt, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.height, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.colthr, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.columns, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.WIN, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.LOSE, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.ABSENT, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.posed, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.rows, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.dias, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.htindex, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.PROBES, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.ht, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.lock, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.he, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.stride, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.TRANSIZE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.NSAMELOCK, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.STRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.INTMODSTRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.DRAW, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.DRAWLOSE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.DRAWWIN, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.history, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.moves, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.hits, Pre-Permissions=share, Post Permissions=share
Method Name = wins
Ref-Var= sb.rows, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.dias, Pre-Permissions=share, Post Permissions=share
Method Name = transpose
Ref-Var= sb.htindex, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.PROBES, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.ht, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.lock, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.he, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.stride, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.TRANSIZE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.ABSENT, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.columns, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.NSAMELOCK, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.STRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.INTMODSTRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Method Name = hash
Ref-Var= sb.columns, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.lock, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.htindex, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.TRANSIZE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.stride, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.NSAMELOCK, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.STRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.INTMODSTRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Method Name = ab
Ref-Var= sb.LOSE, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.WIN, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.nodes, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.plycnt, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.DRAW, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.height, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.colthr, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.columns, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.ABSENT, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.DRAWLOSE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.DRAWWIN, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.posed, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.history, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.rows, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.dias, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.moves, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.htindex, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.PROBES, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.ht, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.lock, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.he, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.stride, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.TRANSIZE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.NSAMELOCK, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.STRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.INTMODSTRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.hits, Pre-Permissions=share, Post Permissions=share
Method Name = makemove
Ref-Var= sb.moves, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.plycnt, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.height, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.columns, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.rows, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.dias, Pre-Permissions=share, Post Permissions=share
Method Name = backmove
Ref-Var= sb.plycnt, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.moves, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.height, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.columns, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.rows, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.dias, Pre-Permissions=share, Post Permissions=share
Method Name = transrestore
Ref-Var= sb.posed, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.htindex, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.PROBES, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.ht, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.lock, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.hits, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.he, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.stride, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.TRANSIZE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.columns, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.NSAMELOCK, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.STRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.INTMODSTRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Method Name = transput
Ref-Var= sb.htindex, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.PROBES, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.he, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.hits, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.ht, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.lock, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.stride, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.TRANSIZE, Pre-Permissions=pure, Post Permissions=pure
Method Name = transtore
Ref-Var= sb.posed, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.columns, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.lock, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.htindex, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.TRANSIZE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.stride, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.NSAMELOCK, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.STRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.INTMODSTRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.PROBES, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.he, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.hits, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.ht, Pre-Permissions=pure, Post Permissions=pure

//////////////////////////////////////////////////////


//////////////////////////////////////////////////////

//////////////////////////////////////////////////////
Class Name = ConnectFourConstants

//////////////////////////////////////////////////////


//////////////////////////////////////////////////////

//////////////////////////////////////////////////////
Class Name = Game
Method Name = Game
Ref-Var= search.Game.moves, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.rows, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.dias, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.columns, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.height, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.plycnt, Pre-Permissions=share, Post Permissions=share
Method Name = reset
Ref-Var= search.Game.plycnt, Pre-Permissions=share, Post Permissions=share
Ref-Var= search.Game.dias, Pre-Permissions=share, Post Permissions=share
Ref-Var= search.Game.columns, Pre-Permissions=share, Post Permissions=share
Ref-Var= search.Game.height, Pre-Permissions=share, Post Permissions=share
Ref-Var= search.Game.rows, Pre-Permissions=share, Post Permissions=share
Method Name = toString
Ref-Var= search.Game.plycnt, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= search.Game.moves, Pre-Permissions=pure, Post Permissions=pure
Method Name = wins
Ref-Var= sb.rows, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.dias, Pre-Permissions=share, Post Permissions=share
Method Name = backmove
Ref-Var= sb.plycnt, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.moves, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.height, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.columns, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.rows, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.dias, Pre-Permissions=share, Post Permissions=share
Method Name = makemove
Ref-Var= sb.moves, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.plycnt, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.height, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.columns, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.rows, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.dias, Pre-Permissions=share, Post Permissions=share

//////////////////////////////////////////////////////


//////////////////////////////////////////////////////

//////////////////////////////////////////////////////
Class Name = TransGame
Method Name = TransGame
Ref-Var= search.TransGame.ht, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.TransGame.TRANSIZE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= search.TransGame.he, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.moves, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.rows, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.dias, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.columns, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.height, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.plycnt, Pre-Permissions=share, Post Permissions=share
Method Name = emptyTT
Ref-Var= sb.TRANSIZE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.he, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.posed, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.hits, Pre-Permissions=share, Post Permissions=share
Method Name = hitRate
Ref-Var= search.TransGame.posed, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= search.TransGame.hits, Pre-Permissions=pure, Post Permissions=pure
Method Name = hash
Ref-Var= sb.columns, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.lock, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.htindex, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.TRANSIZE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.stride, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.NSAMELOCK, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.STRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.INTMODSTRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Method Name = transpose
Ref-Var= sb.htindex, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.PROBES, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.ht, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.lock, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.he, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.stride, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.TRANSIZE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.ABSENT, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.columns, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.NSAMELOCK, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.STRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.INTMODSTRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Method Name = result
Ref-Var= search.TransGame.ABSENT, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.htindex, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.PROBES, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.ht, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.lock, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.he, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.stride, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.TRANSIZE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.ABSENT, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.columns, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.NSAMELOCK, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.STRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.INTMODSTRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Method Name = transrestore
Ref-Var= sb.posed, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.htindex, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.PROBES, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.ht, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.lock, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.hits, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.he, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.stride, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.TRANSIZE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.columns, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.NSAMELOCK, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.STRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.INTMODSTRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Method Name = transtore
Ref-Var= sb.posed, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.columns, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.lock, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.htindex, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.TRANSIZE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.stride, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.NSAMELOCK, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.STRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.INTMODSTRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.PROBES, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.he, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.hits, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.ht, Pre-Permissions=pure, Post Permissions=pure
Method Name = transput
Ref-Var= sb.htindex, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.PROBES, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.he, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.hits, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.ht, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.lock, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.stride, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.TRANSIZE, Pre-Permissions=pure, Post Permissions=pure
Method Name = htstat
Ref-Var= search.ConnectFourConstants.TRANSIZE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= search.ConnectFourConstants.he, Pre-Permissions=share, Post Permissions=share
Ref-Var= search.ConnectFourConstants.LOSE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= search.ConnectFourConstants.DRAWLOSE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= search.ConnectFourConstants.DRAW, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= search.ConnectFourConstants.DRAWWIN, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= search.ConnectFourConstants.WIN, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= search.TransGame.posed, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= search.TransGame.hits, Pre-Permissions=pure, Post Permissions=pure

//////////////////////////////////////////////////////

Class Name = JGFSearchBench
Method Name = JGFsetsize
Ref-Var= sb.size, Pre-Permissions=share, Post Permissions=share
Method Name = JGFinitialise
Ref-Var= sb.startingMoves, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.size, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= search.Game.plycnt, Pre-Permissions=share, Post Permissions=share
Ref-Var= search.Game.dias, Pre-Permissions=share, Post Permissions=share
Ref-Var= search.Game.columns, Pre-Permissions=share, Post Permissions=share
Ref-Var= search.Game.height, Pre-Permissions=share, Post Permissions=share
Ref-Var= search.Game.rows, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.moves, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.plycnt, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.height, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.columns, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.rows, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.dias, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.TRANSIZE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.he, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.posed, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.hits, Pre-Permissions=share, Post Permissions=share
Method Name = JGFapplication
Ref-Var= sb.nodes, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.msecs, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.plycnt, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.height, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.colthr, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.columns, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.WIN, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.LOSE, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.ABSENT, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.posed, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.rows, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.dias, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.htindex, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.PROBES, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.ht, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.lock, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.he, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.stride, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.TRANSIZE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.NSAMELOCK, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.STRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.INTMODSTRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.DRAW, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.DRAWLOSE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.DRAWWIN, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.history, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.moves, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.hits, Pre-Permissions=share, Post Permissions=share
Method Name = JGFvalidate
Ref-Var= sb.TRANSIZE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.he, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.size, Pre-Permissions=pure, Post Permissions=pure
Method Name = JGFtidyup
Ref-Var= sb.ht, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.he, Pre-Permissions=none, Post Permissions=unique
Method Name = JGFrun
Ref-Var= sb.size, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.startingMoves, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= search.Game.plycnt, Pre-Permissions=share, Post Permissions=share
Ref-Var= search.Game.dias, Pre-Permissions=share, Post Permissions=share
Ref-Var= search.Game.columns, Pre-Permissions=share, Post Permissions=share
Ref-Var= search.Game.height, Pre-Permissions=share, Post Permissions=share
Ref-Var= search.Game.rows, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.moves, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.plycnt, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.height, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.columns, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.rows, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.dias, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.TRANSIZE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.he, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.posed, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.hits, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.nodes, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.msecs, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.colthr, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.WIN, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.LOSE, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.ABSENT, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.htindex, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.PROBES, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.ht, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.lock, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.stride, Pre-Permissions=share, Post Permissions=share
Ref-Var= sb.NSAMELOCK, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.STRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.INTMODSTRIDERANGE, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= sb.DRAW, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.DRAWLOSE, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.DRAWWIN, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= sb.history, Pre-Permissions=share, Post Permissions=share
Method Name = main
Ref-Var= search.TransGame.history, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.TRANSIZE, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.PROBES, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.REPORTPLY, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.UNK, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.LOSE, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.DRAWLOSE, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.DRAW, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.DRAWWIN, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.WIN, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.EMPTY, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.BLACK, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.WHITE, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.EDGE, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.ConnectFourConstants.startingMoves, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.colthr, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.colwon, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.NSAMELOCK, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.STRIDERANGE, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.INTMODSTRIDERANGE, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.ABSENT, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.size, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.startingMoves, Pre-Permissions=none, Post Permissions=none
Ref-Var= search.Game.plycnt, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.dias, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.columns, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.height, Pre-Permissions=none, Post Permissions=unique
Ref-Var= search.Game.rows, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.moves, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.plycnt, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.height, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.columns, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.rows, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.dias, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.TRANSIZE, Pre-Permissions=none, Post Permissions=none
Ref-Var= sb.he, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.posed, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.hits, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.nodes, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.msecs, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.colthr, Pre-Permissions=none, Post Permissions=none
Ref-Var= sb.WIN, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.LOSE, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.ABSENT, Pre-Permissions=none, Post Permissions=none
Ref-Var= sb.htindex, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.PROBES, Pre-Permissions=none, Post Permissions=none
Ref-Var= sb.ht, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.lock, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.stride, Pre-Permissions=none, Post Permissions=unique
Ref-Var= sb.NSAMELOCK, Pre-Permissions=none, Post Permissions=none
Ref-Var= sb.STRIDERANGE, Pre-Permissions=none, Post Permissions=none
Ref-Var= sb.INTMODSTRIDERANGE, Pre-Permissions=none, Post Permissions=none
Ref-Var= sb.DRAW, Pre-Permissions=none, Post Permissions=none
Ref-Var= sb.DRAWLOSE, Pre-Permissions=none, Post Permissions=none
Ref-Var= sb.DRAWWIN, Pre-Permissions=none, Post Permissions=none
Ref-Var= sb.history, Pre-Permissions=none, Post Permissions=unique*/