/*
 * Vector Racer 4K 
 * By Harm Boschloo
 * Vector racer is a turn-based racing game best player against
 * friends. Use the arrow and enter keys to navigate and play. 
 * Press escape to return to the menu or exit.
 * todo:
 * improve sim loop
 * use getRaster... for track generation
 * check 4kminers game (for draw loop etc.)
 */

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.image.BufferedImage;
import java.awt.image.BufferStrategy;
//import java.awt.image.DataBufferInt;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import javax.swing.JFrame;


/*
 * Class for Vector Racer 4k
 */
class V extends JFrame
{
	private static boolean[] _keys = new boolean[65536];
	private static int _ai_best_tv; // best ai trace value
	private static int _du = 0; // change of current player speed in x direction 
	private static int _dv = 0; // change of current player speed in y direction 
	
	/*
	 * Capture key events
	 */
	protected void processKeyEvent( KeyEvent e ) {
        switch ( e.getID() ) {
            case KeyEvent.KEY_PRESSED:
                _keys[((KeyEvent)e).getKeyCode()] = true;
        }
    }
    
	/*
	 * Entry point.
	 */
	public static void main( String argv[] ) {
		new V();
	}
	
	/*
	 * Vector Racer 4k constructor
	 */
	private V()	{
        super( "Vector Racer 4k" );
		//show();
		setVisible( true );
		 
		// ##################################################
		// Final variables
		// ##################################################
		
		// layout
		final int TRACK_WIDTH = 680;
		final int MENU_WIDTH = 110;
		final int WIDTH = TRACK_WIDTH+MENU_WIDTH;
		final int HEIGHT = 500;
		final int TOP = getInsets().top;
		final int SIDE = getInsets().left;
		final int BOTTOM = getInsets().bottom;

		// states
		final int MENU_STATE = 1;
		final int INPUT_STATE = 2;
		final int NEW_TRACK_STATE = 3;
		final int PROCESS_STEP_STATE = 4;
		final int NEW_RACE_STATE = 5;
	    final int PROCESS_STATE = 6;
		final int AI_INPUT_STATE = 7;
		final int SIM_STATE = 8;
		
		// menu
		final int MENU_RACE = 30;
		final int MENU_PLAYERS = 50;
		final int MENU_AIS = 70;
		final int MENU_AI_LEVEL = 90;
		final int MENU_NEW_TRACK = 110;

		// track
		final int WALL = -2;
		final int START_FINISH = -1;
		final int GRID_SIZE = 15;
		final int TRACK_SIZE = 70;		
		final Color TRACK_COLOR = new Color( 90,90,90 );
		final Color SF_COLOR = new Color( 255,255,255 );
		final Color GRID_COLOR = new Color( 200,200,200,100 );
		final  int SF_X = 330; // start/finish x position; multiple of GRIDSIZE!
		final  int SF_Y = 120; // start/finish y position; multiple of GRIDSIZE!

		// players
		final int MAX_PLAYERS = 10;
		final int PLAYER_R = 4;
		final Color[] PLAYER_COLORS = {
	        	new Color( 255,0,0 ),
	        	new Color( 255,255,0 ),
	        	new Color( 0,200,0 ),
	        	new Color( 255,100,255 ),
	        	new Color( 255,100,0 ),
	        	new Color( 255,255,255 ),
	        	new Color( 50,200,255 ),
	        	new Color( 255,200,100 ),
	        	new Color( 0,200,150 ),
	        	new Color( 150,255,100 ),
		};
		
		// ##################################################
		// Frame setup
		// ##################################################
        setSize( WIDTH+2*SIDE,HEIGHT+TOP+BOTTOM );
        setResizable( false );
       
        // ##################################################
		// Variables
		// ##################################################
		// state
		int state = NEW_TRACK_STATE;
		
		// menu
		int menu = MENU_RACE; // current menu indicator position
		int h_n = 1; // number of human players
		int ai_n = 3; // number of ai players
		int ai_level = 2; // level of the ai

		// race
		long time = 0; // time
		long time0 = 0; // time at start of simulation
		long timem0 = 0; // time in millies at start of simulation
		int[][] trace = new int[WIDTH][HEIGHT]; // track trace array
		int halfway_tv = 0; // halfway trace value
		double best = 0;
		int message_pos = 0; // current message position
		int currentPlayer = 0;
		
		// players
		int[] p_x = new int[MAX_PLAYERS]; // player x position 
		int[] p_y = new int[MAX_PLAYERS]; // player y position 
		int[] p_x0 = new int[MAX_PLAYERS]; // player x position at start of simulation
		int[] p_y0 = new int[MAX_PLAYERS]; // player y position at start of simulation
		int[] p_u = new int[MAX_PLAYERS]; // player speed in x direction 
		int[] p_v = new int[MAX_PLAYERS]; // player speed in y direction 
		int[] p_i = new int[MAX_PLAYERS]; // player pixel count during simulation
		boolean[] p_halfway = new boolean[MAX_PLAYERS]; // player halfway indicator

		// graphics
		createBufferStrategy(2);
		BufferStrategy strategy = getBufferStrategy();
		BufferedImage bg = new BufferedImage( WIDTH,HEIGHT,BufferedImage.TYPE_INT_ARGB );
		BufferedImage track = new BufferedImage( WIDTH,HEIGHT,BufferedImage.TYPE_INT_ARGB );
		BufferedImage overlay = null;
		Graphics g = bg.getGraphics();
		Graphics2D g2 = (Graphics2D)track.getGraphics();

		// ##################################################
		// Create background image
		// ##################################################
		// background
		g.setColor( new Color( 0,0,0 ) );
		for ( int x = 0; x < WIDTH; x++ ) {
			for ( int y = 0; y < HEIGHT; y++ ) {
				g.setColor( new Color( random(190,210),random(190,210),random(140,160) ) );
				g.fillRect( x,y,1,1 );							
			}
		}
		
		// menu background
		g.setColor( new Color( 0,0,0,150 ) );
		g.fillRect( TRACK_WIDTH,0,MENU_WIDTH,HEIGHT );			
		
		// ##################################################
		// Main loop
		// ##################################################
		while( true ) {

			// ##################################################
			// Handle key events
			// ##################################################
			
			// player selection
			if ( state == INPUT_STATE  ) {
				if ( _keys[KeyEvent.VK_UP] ) {
					_dv -= GRID_SIZE;
				}
				if ( _keys[KeyEvent.VK_DOWN] ) {
					_dv += GRID_SIZE;
				}
				if ( _keys[KeyEvent.VK_RIGHT] ) {
					_du += GRID_SIZE;
				}
				if ( _keys[KeyEvent.VK_LEFT] ) {
					_du -= GRID_SIZE;
				}
				if ( _du > GRID_SIZE ) { _du = GRID_SIZE; }
				if ( _du < -GRID_SIZE ) { _du = -GRID_SIZE; }
				if ( _dv > GRID_SIZE ) { _dv = GRID_SIZE; }
				if ( _dv < -GRID_SIZE ) { _dv = -GRID_SIZE; }
				
				if ( _keys[KeyEvent.VK_ENTER] ) {
					state = PROCESS_STEP_STATE;				
				}
			}	
				
			// main menu navigation
			if ( state == MENU_STATE ) {
				// up
				if ( _keys[KeyEvent.VK_UP] ) {
					if ( menu == MENU_RACE ) {
						menu = MENU_NEW_TRACK;
					}
					else {
						menu -= 20;
					}
				}
				// down
				if ( _keys[KeyEvent.VK_DOWN] ) {
					if ( menu == MENU_NEW_TRACK ) {
						menu = MENU_RACE;
					}
					else {
						menu += 20;
					}
				}	
				// right
				if ( _keys[KeyEvent.VK_RIGHT] ) {
					if ( menu == MENU_PLAYERS && (h_n+ai_n) < MAX_PLAYERS ) {
						h_n++;
					}
					if ( menu == MENU_AIS && (h_n+ai_n) < MAX_PLAYERS ) {
						ai_n++;
					}
					if ( menu == MENU_AI_LEVEL && ai_level < 4 ) {
						ai_level++;
					}
				}
				// left
				if ( _keys[KeyEvent.VK_LEFT] ) {
					if ( menu == MENU_PLAYERS && h_n > 0 ) {
						h_n--;
					}
					if ( menu == MENU_AIS && ai_n > 0 ) {
						ai_n--;
					}
					if ( menu == MENU_AI_LEVEL && ai_level > 2 ) {
						ai_level--;
					}
				}
				// enter
				if ( _keys[KeyEvent.VK_ENTER] ) {
					if ( menu == MENU_RACE ) {
						state = NEW_RACE_STATE;
					}
					
					if ( menu == MENU_NEW_TRACK ) {
						state = NEW_TRACK_STATE;
					}
				}
				// escape
				if ( _keys[KeyEvent.VK_ESCAPE] ) {
					System.exit( 0 );
				}
			}
			
			// return to main menu from race
			if ( _keys[KeyEvent.VK_ESCAPE] ) 
			{
				state = MENU_STATE;
			}
			
			// clear keys
			//_keys = new boolean[65536];
			Arrays.fill( _keys, false );
			
			// ##################################################
			// Create new track
			// ##################################################
			if ( state == NEW_TRACK_STATE ) {

				// new overlay
				overlay = new BufferedImage( WIDTH,HEIGHT,BufferedImage.TYPE_INT_ARGB );
				
				// reset best score
				best = 1000;
				
				// create track
				final int MRGN = TRACK_SIZE/2+2;
				final int Y1 = SF_Y+MRGN;
				final int Y2 = SF_Y+3*MRGN;
				final int X2 = SF_X-2*GRID_SIZE-MRGN;
				final int X3 = SF_X+MRGN;
				final int X1 = X2/2;
				final int X4 = X3+(TRACK_WIDTH-X3)/2;
				final int[] x = {
					X2,
					X3,
					// top right
					random( X3+MRGN,X4-MRGN ),
					random( X4,TRACK_WIDTH-MRGN ),
					// bottom right
					random( X4+MRGN,TRACK_WIDTH-MRGN ),
					random( X3+MRGN,X4-MRGN ),
					// bottom middle
					random( X2+MRGN,X3-MRGN ),
					random( X2+MRGN,X3-MRGN ),
					// bottom left
					random( MRGN,X2-MRGN ),
					random( MRGN,X1-MRGN ),
					// top left
					random( MRGN,X1-MRGN ),
					random( X1+MRGN,X2 ),
				};
				final int[] y = {
					SF_Y,
					SF_Y,
					// top right
					random( MRGN,Y2-MRGN ),
					random( MRGN,Y1-MRGN ),
					// bottom right
					random( Y2+MRGN,HEIGHT-MRGN ),
					random( Y2+MRGN,HEIGHT-MRGN ),
					// bottom middle
					random( Y2+MRGN,HEIGHT-MRGN ),
					random( Y1+MRGN,HEIGHT-MRGN ),
					// bottom left
					random( Y2+MRGN,HEIGHT-MRGN ),
					random( Y1+MRGN,Y2-MRGN ),
					// top left
					random( MRGN,Y1-MRGN ),
					random( MRGN,Y1-MRGN ),		
				};
				final int N = 12;
				
				// draw background / clear previous track
				g2.drawImage( bg, 0, 0, null );

				// draw lines/curbs
				g2.setColor( new Color( 254,254,254 ) );
				g2.setStroke( new BasicStroke( TRACK_SIZE+4,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND ) );
				g2.drawPolygon( x,y,N );
				
				final float[] DASH = { 6 };
				g2.setColor( new Color( 254,0,0 ) );
				g2.setStroke( new BasicStroke(
					TRACK_SIZE+4,
	                BasicStroke.CAP_BUTT, 
	                BasicStroke.JOIN_ROUND, 
	                0, 
	                DASH, 
	                0 ) );
				g2.drawPolygon( x,y,N );
				
				g2.setStroke( new BasicStroke(
						5,
		                BasicStroke.CAP_BUTT, 
		                BasicStroke.JOIN_ROUND, 
		                0, 
		                DASH, 
		                0 ) );
				for ( int i = 0; i < N; i++ ) {
					g2.drawOval( x[i]-(TRACK_SIZE+2)/2,y[i]-(TRACK_SIZE+2)/2,TRACK_SIZE+2,TRACK_SIZE+2 );
				}
				
				g2.setColor( new Color( 254,254,254 ) );
				g2.setStroke( new BasicStroke(
						5,
		                BasicStroke.CAP_BUTT, 
		                BasicStroke.JOIN_ROUND, 
		                0, 
		                DASH, 
		                5 ) );
				for ( int i = 0; i < N; i++ ) {
					g2.drawOval( x[i]-(TRACK_SIZE+2)/2,y[i]-(TRACK_SIZE+2)/2,TRACK_SIZE+2,TRACK_SIZE+2 );
				}
				
				// draw track
				g2.setColor( TRACK_COLOR );
				g2.setStroke( new BasicStroke( TRACK_SIZE,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND ) );
				g2.drawPolygon( x,y,N );
				
				// draw start/finish
				final int[] sfx = { SF_X,SF_X-2*GRID_SIZE,SF_X-2*GRID_SIZE };
				final int[] sfy = { SF_Y,SF_Y+TRACK_SIZE/2+1,SF_Y-TRACK_SIZE/2-1 };
				g2.setColor( SF_COLOR );
				g2.setStroke( new BasicStroke() );
				g2.fillPolygon( sfx,sfy,3 );
				
				// init trace & draw grid & draw bg
				//int[] pixels = ((DataBufferInt)track.getRaster().getDataBuffer()).getData();
				for ( int xx = 0; xx < TRACK_WIDTH; xx++ ) {
					for ( int yy = 0; yy < HEIGHT; yy++ ) {
						Color color = new Color( track.getRGB( xx,yy ) );
						trace[xx][yy] = WALL;
						if ( color.equals( SF_COLOR ) ) { // start/finish
							trace[xx][yy] = START_FINISH;
							int c = random( 230,250 );
							g2.setColor( new Color( c,c,c ) );
							g2.fillRect( xx,yy,1,1 );
							if ( (double)xx%(double)GRID_SIZE == 0.0 || (double)yy%(double)GRID_SIZE == 0.0 ) {
								g2.setColor( GRID_COLOR );
								g2.fillRect( xx,yy,1,1 );
			                }
						}
						if ( color.equals( TRACK_COLOR ) ) {
							trace[xx][yy] = 0;
							final int c = random( 80,100 );
							g2.setColor( new Color( c,c,c ) );
							g2.fillRect( xx,yy,1,1 );
			                if ( (double)xx%(double)GRID_SIZE == 0.0 || (double)yy%(double)GRID_SIZE == 0.0 ) {
			                	g2.setColor( GRID_COLOR );
			                	g2.fillRect( xx,yy,1,1 );
			                }					
						}

					}
				}
				state = MENU_STATE;
			}
			
			// ##################################################
			// set up new race
			// ##################################################
			if ( state == NEW_RACE_STATE ) {
				state = PROCESS_STATE;

				// init players
				for( int i = 0; i < MAX_PLAYERS; i++ ) {
					if ( i < (h_n+ai_n) ) {
						p_x[i] = SF_X;
						p_y[i] = SF_Y;
					}
					else {
						p_x[i] = 0;
						p_y[i] = 0;
					}
					p_u[i] = 0;
					p_v[i] = 0;
					p_halfway[i] = false;
				}
				
				// misc inits
				currentPlayer = 0;
				time = 0;
				message_pos = 170;

		        int[][] currentPoints = new int[2][1000]; // should be larger than about 250
		        int[][] changedPoints = new int[2][1000];
		        int i,d,lx,ly,lv,x,y;
		        int[] dx = { -1,1,0,0 };
		        int[] dy = { 0,0,-1,1 };  
		        final int dn = 4;
		        final int X = 0;
		        final int Y = 1;
		        currentPoints[X][0] = SF_X-2*GRID_SIZE-1; // -1, else we remove a finish pixel
		        currentPoints[Y][0] = SF_Y;
		        int nCurrent = 1;
		        int nChanged = 0;
		        trace[currentPoints[0][0]][currentPoints[1][0]] = 1;
		        while ( nCurrent > 0 ) {
		            for ( i = 0; i < nCurrent; i++ ) {
		            	x = currentPoints[X][i];
		            	y = currentPoints[Y][i];
		                // check around
		            	for ( d = 0; d < dn; d++ ) {
			            	lx = x+dx[d];
			            	ly = y+dy[d];
		            		lv = trace[lx][ly];
		                    if ( lv == 0 || lv > trace[x][y]+1 ) {
		                    	trace[lx][ly] = trace[x][y]+1;
		                    	changedPoints[X][nChanged] = lx;
		                    	changedPoints[Y][nChanged] = ly;
		                    	nChanged++;
		                    }
		                } // end for loop
		            } // end for loop
		            // reset points
		            int[][] tmp = currentPoints;
		            currentPoints = changedPoints;
		            changedPoints = tmp;
		            nCurrent = nChanged;
		            nChanged = 0;
		        }  // end while loop
				
		        halfway_tv = (trace[SF_X+GRID_SIZE][SF_Y])/2;
		        
		        // new overlay
				overlay = new BufferedImage( WIDTH,HEIGHT,BufferedImage.TYPE_INT_ARGB );
				
				// player input
				if ( currentPlayer < h_n ) {
					state = INPUT_STATE;			 
				}
				else {
					state = AI_INPUT_STATE;
				}
			}
			
			// ##################################################
			// step processing
			// ##################################################
			if ( state == PROCESS_STEP_STATE ) {
				
				state = PROCESS_STATE;
				p_u[currentPlayer] += _du;
				p_v[currentPlayer] += _dv;
				
				_du = 0;
				_dv = 0;
				
				// next player
				while( true ) {
					currentPlayer++;
					if ( (h_n+ai_n) == currentPlayer ) {
						currentPlayer = 0;

						for( int i = 0; i < MAX_PLAYERS; i++ ) {
							p_i[i] = 0;
							p_x0[i] = p_x[i];
							p_y0[i] = p_y[i];
						}
						time0 = time;
						timem0 = System.currentTimeMillis();
						
						state = SIM_STATE;
						break;
					}
					else if ( p_x[currentPlayer] != 0 ) {
						 if ( currentPlayer < h_n ) {
							 state = INPUT_STATE;			 
						 }
						 else {
							 state = AI_INPUT_STATE;
						 }
						break;
					}
				}
			}
			
			// ##################################################
			// do simulation
			// ##################################################
			if ( state == SIM_STATE ) {

				// current time
				long t = time;
				time = time + 2*(System.currentTimeMillis()-timem0); // speed-up 2x
				
				if ( time > time0 + 1000 ) {
					time = time0 + 1000;
				}
				
				// run untill current time reached
				g = overlay.getGraphics();
				for ( ; t <= time; t += 10 ) {
					for( int i = 0; i < MAX_PLAYERS; i++ ) {
						if ( p_x[i] != 0 ) {
							int pixs = Math.max( Math.abs( p_u[i] ), Math.abs( p_v[i] ) );
							double dx = ( (double)p_u[i] ) / ( (double)pixs );
							double dy = ( (double)p_v[i] ) / ( (double)pixs );
							
							while( time0 + 1000.0*(p_i[i]/(double)pixs) < time ) {
			
								p_i[i]++;
								p_x[i] = p_x0[i] + (int)Math.round( (p_i[i])*dx );
								p_y[i] = p_y0[i] + (int)Math.round( (p_i[i])*dy );
								
								g.setColor( new Color( 
									PLAYER_COLORS[i].getRed(),
									PLAYER_COLORS[i].getGreen(),
									PLAYER_COLORS[i].getBlue(),
									10 ) );
								g.fillOval( p_x[i]-PLAYER_R, p_y[i]-PLAYER_R, 2*PLAYER_R, 2*PLAYER_R );
								
								int v = trace[p_x[i]][p_y[i]];
								//System.out.println( "trace value: " + v );
								
								// check halfway [todo, check 1/3, 2/3]
								if ( Math.abs( v-halfway_tv ) < 10 ) {
									p_halfway[i] = true;
								}
													
								// check surface
								g.setColor( PLAYER_COLORS[i] );
								if ( v == WALL ) { // wall
									g.drawString( "player " + String.valueOf( i+1 ) + " crashed", TRACK_WIDTH+10, message_pos );
									message_pos += 15;
									p_x[i] = 0; // out of game
									//System.out.println( "p " + i + " hit wall" );
									break;
								}
								if ( v == START_FINISH && p_halfway[i] ) { // start/finish && finished
									double dtime = Math.round( time0 + 1000.0*(p_i[i]/(double)pixs) )/1000.0;
									if ( dtime < best ) { best = dtime; }
									g.drawString( "player " + String.valueOf( i+1 ) + ": " + String.valueOf( dtime ) , TRACK_WIDTH+10, message_pos );
									message_pos += 15;
									//System.out.println( "p " + i + " finished" );
									p_x[i] = 0; // out of game
									break;
								}
							}
						}
					}
				}

				// check how many players are still racing
				int n = 0;
				for( int i = 0; i < MAX_PLAYERS; i++ ) {
					if ( p_x[i] != 0 ) { 
						n++;
					}
				}
				
				if ( n == 0 ) { // no players racing, return to main menu
					state = MENU_STATE;
				}
				else if ( time == time0+1000 ) { // simulation step finished
					// find next player
					while ( p_x[currentPlayer] == 0 ) {
						currentPlayer++;
					}
					// change to (ai) input state 
					if ( currentPlayer < h_n ) {
						state = INPUT_STATE;			 
					}
					else {
						state = AI_INPUT_STATE;
					}
				}
			}
			
			// ##################################################
			// run ai
			// ##################################################
			if ( state == AI_INPUT_STATE ) {
				state = PROCESS_STATE;
				_ai_best_tv = Integer.MAX_VALUE;
				runAI( 
					p_x[currentPlayer], 
					p_y[currentPlayer],
					p_u[currentPlayer], 
					p_v[currentPlayer],
					0,
					0,
					ai_level,
					ai_level,
					trace,
					halfway_tv,
					p_halfway[currentPlayer]);
				
				state = PROCESS_STEP_STATE;
			}
			

			// ##################################################
			// Draw graphics
			// ##################################################
			g = strategy.getDrawGraphics();
			g.translate( SIDE,TOP );
			// draw track
			g.drawImage( track, 0, 0, null );
			
			// draw overlay
			g.drawImage( overlay, 0, 0, null );

			// draw main menu
			g.translate( TRACK_WIDTH,0 );
			if ( state == MENU_STATE || state == NEW_TRACK_STATE ) {
				g.setColor( PLAYER_COLORS[MAX_PLAYERS-1] );
				g.drawOval( 15,menu-8,8,8 );
				g.drawString( "race",28,MENU_RACE );
				g.drawString( "players: " + String.valueOf( h_n ),28,MENU_PLAYERS );
				g.drawString( "ais: " + String.valueOf( ai_n ),28,MENU_AIS );
				g.drawString( "ai level: " + String.valueOf( (ai_level-1) ),28,MENU_AI_LEVEL );
				g.drawString( "new track",28,MENU_NEW_TRACK );
			}
			else {
				// draw time
				g.setColor( PLAYER_COLORS[MAX_PLAYERS-1] );
				g.drawString( "time: " + String.valueOf( time/1000.0 ),20,30 );
			}
			
			if ( best < 1000 ) {
				// draw best time
				g.setColor( PLAYER_COLORS[MAX_PLAYERS-1] );
				g.drawString( "best: " + String.valueOf( best ),10,150 );
			}
			g.translate( -TRACK_WIDTH,0 );
		

			// draw players
			if ( state != MENU_STATE ) {
				for ( int i = 0; i < MAX_PLAYERS; i++ ) {
					if ( p_x[i] != 0 ) {
						g.setColor( PLAYER_COLORS[i] );
						g.drawOval( p_x[i]-PLAYER_R,p_y[i]-PLAYER_R,2*PLAYER_R,2*PLAYER_R );
					}
				}
			}
			
			// draw input selection
			if ( state == INPUT_STATE ) {
				g.setColor( PLAYER_COLORS[currentPlayer] );
				g.drawString( "player: " + String.valueOf( currentPlayer+1 ),TRACK_WIDTH+20,50 );
				g.fillOval( p_x[currentPlayer]-PLAYER_R-1,p_y[currentPlayer]-PLAYER_R-1,2*PLAYER_R+2,2*PLAYER_R+2 );
				
				// drive line
				g.drawLine( 
					p_x[currentPlayer],
					p_y[currentPlayer],
					p_x[currentPlayer]+p_u[currentPlayer]+_du,
					p_y[currentPlayer]+p_v[currentPlayer]+_dv );
				
				// input options
				g.setColor( new Color( 
					PLAYER_COLORS[currentPlayer].getRed(),
					PLAYER_COLORS[currentPlayer].getGreen(),
					PLAYER_COLORS[currentPlayer].getBlue(),
					150 ) );
				for ( int dx = -1; dx < 2; dx++ ) {
					for ( int dy = -1; dy < 2; dy++ ) {
						int x = p_x[currentPlayer]+p_u[currentPlayer]+GRID_SIZE*dx;
						int y = p_y[currentPlayer]+p_v[currentPlayer]+GRID_SIZE*dy;;
						g.drawOval( x-PLAYER_R,y-PLAYER_R,2*PLAYER_R,2*PLAYER_R );
					}
				}
			}
			
			// draw graphics to screen
			strategy.show();
			if ( !isVisible() ) {
				System.exit( 0 );
			}
			
			// ##################################################
			// Sleep for a short while
			// ##################################################
			try {
				Thread.sleep( 10 );
			}
			catch( Exception e ) {
				System.exit( 0 );
			}

		}
	}
	
	/*
	 * Runs the ai
	 */
	private void runAI( int x0, int y0, int u0, int v0, int du0, int dv0, int n,
			int ai_level, int[][] trace, int halfway_tv, boolean halfway ) {
		final int GRID_SIZE = 15;
		final int START_FINISH = -1;
		final int WALL = -2;
		n--; // 1 iteration level deeper
		int tv = 0; // local trace value
		
		// checks every possible move and stores the best move (with the 'best' trace value)
		for ( int du = -GRID_SIZE; du < 2*GRID_SIZE; du += GRID_SIZE ) {
			for ( int dv = -GRID_SIZE; dv < 2*GRID_SIZE; dv += GRID_SIZE ) {
				// store move at first iteration
				if ( n == (ai_level-1) ) {
					du0 = du;
					dv0 = dv;
				}
				
				// variables
				int u = u0+du;
				int v = v0+dv;
		        int pixs = Math.max( Math.abs( u ), Math.abs( v ) );
				double dx = ( (double)u ) / ( (double)pixs );
				double dy = ( (double)v ) / ( (double)pixs );
				boolean goOn = true;
				int x = x0;
				int y = y0;
				
				// check pixels for move
				for ( int i = 0; i <= pixs; i++ ) {
					x = x0 + (int)Math.round( i*dx );
					y = y0 + (int)Math.round( i*dy );

					// check surface
					tv = trace[x][y];
					if ( tv == WALL ) { // hit a wall
						goOn = false;
						break;
					}
					if ( tv == START_FINISH ) { // start/finish
						if ( halfway ) { // finish reached
							// negavite value will always be better than regular value
							// random to spread out ai's at finish
							int f = -n*100-pixs+random( -1,1 );
							if ( f < _ai_best_tv ) {
								_ai_best_tv = f;
								_du = du0;
								_dv = dv0;
							}
						}
						goOn = false; // no need to look further
						break;
					}
				}
				
				// keep 'speed' of ai's at more or less acceptable level
				// if speed to high, no need to go on
				if ( ai_level < 3 && pixs > random(3,4)*GRID_SIZE ) { goOn = false; }
		        if ( pixs > (ai_level+2)*GRID_SIZE ) { goOn = false; }
		        if ( pixs > (ai_level+random(1,2))*GRID_SIZE ) { goOn = false; }
				
				if ( goOn ) {
					if ( n > 0 ) { // next iteration level
						runAI( x,y,u,v,du0,dv0,n,ai_level,trace,halfway_tv,halfway );
					}
					else { // last iteration level reached
						// check trace value
						if ( tv > 0 ) {
							// introduces some randomness in the first 3/4 of the track
							if ( tv > halfway_tv/2 ) {
								int rndm = GRID_SIZE-2*ai_level;
								tv += random( -rndm,rndm ); 
							}
							// check best trace value and set choosen input
							if ( tv < _ai_best_tv ) {
								_ai_best_tv = tv;
								_du = du0;
								_dv = dv0;
							}
						}								
					}
				}
			}
		}
	}
	
	/*
	 * Random int values between (and including) minimum and maximum
	 */
	private int random( int min, int max ) {
		return min + (int)Math.round( (max-min)*Math.random() );
	}
}