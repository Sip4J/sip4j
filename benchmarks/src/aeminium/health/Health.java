package aeminium.health;

public class Health {
	
	public static final int DEFAULT_THRESHOLD = 5;
	public static int sim_level = 5;
	public static int sim_cities = 12;
	public static int sim_population_ratio = 10;
	public static int sim_time = 365;
	public static int sim_assess_time = 2;
	public static int sim_convalescence_time = 12;
	public static int sim_seed = 23;
	public static double sim_get_sick_p = 0.002; 
	public static double sim_convalescence_p = 0.100;
	public static double sim_realloc_p = 0.150;
	public static long res_population = 7238720;
	public static long res_hospitals = 346201;
	public static long res_personnel = 723872;
	public static long res_checkin = 5267413;
	public static long res_village = 7121156;
	public static long res_waiting = 73923;
	public static long res_assess = 28969;
	public static long res_inside = 14672;
	public static double res_avg_stay =  5.230891;
	
	public static int sim_id;
	
	public static int IQ = 127773;
	
	static Village current;
	
	//Immutable(level), Immutable(Health.sim_population_ratio), Immutable(Health.sim_seed), Immutable(back) -> Immutable(level), Immutable(Health.sim_population_ratio), Immutable(Health.sim_seed), Immutable(back), Unique(result)
	
	/*@Perm(requires="none(this) * pure(#0) in alive",
	ensures="unique(this) * pure(#0) in alive")*/
		public static Village allocateVillage(int level, int vid, Village back) {
		
		return Health.allocateVillage(level, vid, back, false, 0);// health level, village id, village object, aeminium parameter, threshold
	}
	//Immutable(level), Immutable(Health.sim_population_ratio), Immutable(Health.sim_seed), Immutable(back) -> Immutable(level), Immutable(Health.sim_population_ratio), Immutable(Health.sim_seed), Immutable(back), Unique(result)
	
		public static Village allocateVillage(int level, int vid, Village back, boolean isAe, int threshold) {	
		
		int personnel = (int) Math.pow(2, level);// 2 power to health level is assigned to personnel/// we can make seperate functions for this 
		
		int population = personnel * Health.sim_population_ratio;// number of personnel into population ratio // we can make seperate functions for this 
		
		//int population = 3;
		
		//Village current;// do nothing
		/*if (isAe) {
			current = new AeVillage(threshold);
		} else {*/
			current = new Village();//local reference variable creates object of village, create pre none, create unique as post
		//}
		current.id = vid;
		current.level = level;
		current.seed = vid * (IQ + Health.sim_seed);
		current.root = back; // first time assign null to current.root
		
		for (int i = 0; i < population; i++) {
			Patient p = new Patient();// local reference variable, create none as pre and unique as post permission on patient
			p.id = Health.sim_id++;
			p.seed = current.seed;
			p.home_village = current;
			current.population.add(p);// library method call 
		}
		current.hosp.personnel = personnel;
		current.hosp.free_personnel = personnel;
		if (level > 1) {
			for (int i = sim_cities; i > 0; i--) {
				Village curr = Health.allocateVillage(level-1, i, current, isAe, threshold);
				current.children.add(curr);// library method call 
			}
		}
		return current;
	}
	
	/*Class Name = Patient
			Class Name = SeqHealth
			Method Name = main
			Vertex Name = Patient.time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Patient.time_left, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Patient.hosps_visited, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.children, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.population, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.hosp, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.DEFAULT_THRESHOLD, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_level, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_cities, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_population_ratio, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_assess_time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_convalescence_time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_seed, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_get_sick_p, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_convalescence_p, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_realloc_p, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_population, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_hospitals, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_checkin, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_village, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_waiting, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_assess, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_inside, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_avg_stay, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.IQ, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.id, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.level, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.seed, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.root, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_id, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.home_village, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.population, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.hosp, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Health.free_personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.children, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.id, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.level, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.seed, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.root, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.hosps_visited, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.time_left, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.home_village, Post Permissions = none, Pre-Permissions =none
			Vertex Name = SeqHealth.children, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Hosp.inside, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.inside, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Hosp.free_personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.free_personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Hosp.assess, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.assess, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.sim_convalescence_p, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.sim_realloc_p, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.sim_level, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.sim_convalescence_time, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Hosp.waiting, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.waiting, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.sim_assess_time, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Hosp.realloc, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.realloc, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.sim_get_sick_p, Post Permissions = none, Pre-Permissions =none
			Class Name = Village
			Class Name = SeqHealth
			Method Name = main
			Vertex Name = Patient.time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Patient.time_left, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Patient.hosps_visited, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.children, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.population, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.hosp, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.DEFAULT_THRESHOLD, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_level, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_cities, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_population_ratio, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_assess_time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_convalescence_time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_seed, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_get_sick_p, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_convalescence_p, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_realloc_p, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_population, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_hospitals, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_checkin, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_village, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_waiting, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_assess, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_inside, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_avg_stay, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.IQ, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.id, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.level, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.seed, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.root, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_id, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.home_village, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.population, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.hosp, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Health.free_personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.children, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.id, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.level, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.seed, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.root, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.hosps_visited, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.time_left, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.home_village, Post Permissions = none, Pre-Permissions =none
			Vertex Name = SeqHealth.children, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Hosp.inside, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.inside, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Hosp.free_personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.free_personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Hosp.assess, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.assess, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.sim_convalescence_p, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.sim_realloc_p, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.sim_level, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.sim_convalescence_time, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Hosp.waiting, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.waiting, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.sim_assess_time, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Hosp.realloc, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.realloc, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.sim_get_sick_p, Post Permissions = none, Pre-Permissions =none
			Method Name = tick
			Vertex Name = Hosp.inside, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.hosp, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.inside, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.time_left, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Hosp.free_personnel, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.free_personnel, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.population, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Hosp.assess, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.assess, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.seed, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.sim_convalescence_p, Post Permissions = immutable, Pre-Permissions =immutable
			Vertex Name = Village.sim_realloc_p, Post Permissions = immutable, Pre-Permissions =immutable
			Vertex Name = Village.level, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.sim_level, Post Permissions = immutable, Pre-Permissions =immutable
			Vertex Name = Village.sim_convalescence_time, Post Permissions = immutable, Pre-Permissions =immutable
			Vertex Name = Village.time, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.root, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Hosp.waiting, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.waiting, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.sim_assess_time, Post Permissions = immutable, Pre-Permissions =immutable
			Vertex Name = Hosp.realloc, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.realloc, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.id, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.hosps_visited, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.sim_get_sick_p, Post Permissions = immutable, Pre-Permissions =immutable
			Method Name = check_patients_inside
			Vertex Name = Hosp.inside, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.hosp, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.inside, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.time_left, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Hosp.free_personnel, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.free_personnel, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.population, Post Permissions = share, Pre-Permissions =share
			Method Name = check_patients_assess
			Vertex Name = Hosp.assess, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.hosp, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.assess, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.time_left, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.seed, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.sim_convalescence_p, Post Permissions = immutable, Pre-Permissions =immutable
			Vertex Name = Village.sim_realloc_p, Post Permissions = immutable, Pre-Permissions =immutable
			Vertex Name = Village.level, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.sim_level, Post Permissions = immutable, Pre-Permissions =immutable
			Vertex Name = Hosp.inside, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.inside, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.sim_convalescence_time, Post Permissions = immutable, Pre-Permissions =immutable
			Vertex Name = Village.time, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Hosp.free_personnel, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.free_personnel, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.root, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.population, Post Permissions = share, Pre-Permissions =share
			Method Name = check_patients_waiting
			Vertex Name = Hosp.waiting, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.hosp, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.waiting, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Hosp.free_personnel, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.free_personnel, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.time_left, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.sim_assess_time, Post Permissions = immutable, Pre-Permissions =immutable
			Vertex Name = Village.time, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Hosp.assess, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.assess, Post Permissions = pure, Pre-Permissions =pure
			Method Name = check_patients_realloc
			Vertex Name = Hosp.realloc, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.hosp, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.realloc, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.id, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.hosps_visited, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.free_personnel, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.assess, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.time_left, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.sim_assess_time, Post Permissions = immutable, Pre-Permissions =immutable
			Vertex Name = Village.time, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.waiting, Post Permissions = share, Pre-Permissions =share
			Method Name = put_in_hosp
			Vertex Name = Village.hosp, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.hosps_visited, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.free_personnel, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.assess, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.time_left, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.sim_assess_time, Post Permissions = immutable, Pre-Permissions =immutable
			Vertex Name = Village.time, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.waiting, Post Permissions = share, Pre-Permissions =share
			Method Name = check_patients_population
			Vertex Name = Village.population, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.seed, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.sim_get_sick_p, Post Permissions = immutable, Pre-Permissions =immutable
			Vertex Name = Village.hosp, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.hosps_visited, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.free_personnel, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.assess, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.time_left, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.sim_assess_time, Post Permissions = immutable, Pre-Permissions =immutable
			Vertex Name = Village.time, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.waiting, Post Permissions = share, Pre-Permissions =share
			Method Name = displayVillageData
			Vertex Name = Village.id, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.level, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.seed, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.root, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.children, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.population, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.hosps_visited, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.time_left, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.time, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.home_village, Post Permissions = pure, Pre-Permissions =pure
			Method Name = DisplayVillagePatients
			Vertex Name = Village.population, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.id, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.hosps_visited, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.time_left, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.time, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.home_village, Post Permissions = pure, Pre-Permissions =pure
			Class Name = Results
			Class Name = SeqHealth
			Method Name = main
			Vertex Name = Patient.time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Patient.time_left, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Patient.hosps_visited, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.children, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.population, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.hosp, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.DEFAULT_THRESHOLD, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_level, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_cities, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_population_ratio, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_assess_time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_convalescence_time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_seed, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_get_sick_p, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_convalescence_p, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_realloc_p, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_population, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_hospitals, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_checkin, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_village, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_waiting, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_assess, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_inside, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_avg_stay, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.IQ, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.id, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.level, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.seed, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.root, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_id, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.home_village, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.population, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.hosp, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Health.free_personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.children, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.id, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.level, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.seed, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.root, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.hosps_visited, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.time_left, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.home_village, Post Permissions = none, Pre-Permissions =none
			Vertex Name = SeqHealth.children, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Hosp.inside, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.inside, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Hosp.free_personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.free_personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Hosp.assess, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.assess, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.sim_convalescence_p, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.sim_realloc_p, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.sim_level, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.sim_convalescence_time, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Hosp.waiting, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.waiting, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.sim_assess_time, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Hosp.realloc, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.realloc, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.sim_get_sick_p, Post Permissions = none, Pre-Permissions =none
			Class Name = Health
			Class Name = SeqHealth
			Method Name = main
			Vertex Name = Patient.time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Patient.time_left, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Patient.hosps_visited, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.children, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.population, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.hosp, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.DEFAULT_THRESHOLD, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_level, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_cities, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_population_ratio, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_assess_time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_convalescence_time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_seed, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_get_sick_p, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_convalescence_p, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_realloc_p, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_population, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_hospitals, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_checkin, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_village, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_waiting, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_assess, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_inside, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_avg_stay, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.IQ, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.id, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.level, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.seed, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.root, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_id, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.home_village, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.population, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.hosp, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Health.free_personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.children, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.id, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.level, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.seed, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.root, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.hosps_visited, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.time_left, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.home_village, Post Permissions = none, Pre-Permissions =none
			Vertex Name = SeqHealth.children, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Hosp.inside, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.inside, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Hosp.free_personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.free_personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Hosp.assess, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.assess, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.sim_convalescence_p, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.sim_realloc_p, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.sim_level, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.sim_convalescence_time, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Hosp.waiting, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.waiting, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.sim_assess_time, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Hosp.realloc, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.realloc, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.sim_get_sick_p, Post Permissions = none, Pre-Permissions =none
			Method Name = allocate_village
			Vertex Name = Health.sim_level, Post Permissions = immutable, Pre-Permissions =immutable
			Vertex Name = Health.sim_population_ratio, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Health.id, Post Permissions = full, Pre-Permissions =full
			Vertex Name = Health.level, Post Permissions = full, Pre-Permissions =full
			Vertex Name = Health.seed, Post Permissions = full, Pre-Permissions =full
			Vertex Name = Health.IQ, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Health.sim_seed, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Health.root, Post Permissions = full, Pre-Permissions =full
			Vertex Name = Health.sim_id, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.home_village, Post Permissions = full, Pre-Permissions =full
			Vertex Name = Health.population, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Health.personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.hosp, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Health.free_personnel, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Health.sim_cities, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Health.children, Post Permissions = share, Pre-Permissions =share
			Class Name = SeqHealth
			Class Name = SeqHealth
			Method Name = main
			Vertex Name = Patient.time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Patient.time_left, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Patient.hosps_visited, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.children, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.population, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.hosp, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.DEFAULT_THRESHOLD, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_level, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_cities, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_population_ratio, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_assess_time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_convalescence_time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_seed, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_get_sick_p, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_convalescence_p, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_realloc_p, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_population, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_hospitals, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_checkin, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_village, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_waiting, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_assess, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_inside, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_avg_stay, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.IQ, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.id, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.level, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.seed, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.root, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_id, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.home_village, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.population, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.hosp, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Health.free_personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.children, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.id, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.level, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.seed, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.root, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.hosps_visited, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.time_left, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.home_village, Post Permissions = none, Pre-Permissions =none
			Vertex Name = SeqHealth.children, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Hosp.inside, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.inside, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Hosp.free_personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.free_personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Hosp.assess, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.assess, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.sim_convalescence_p, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.sim_realloc_p, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.sim_level, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.sim_convalescence_time, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Hosp.waiting, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.waiting, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.sim_assess_time, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Hosp.realloc, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.realloc, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.sim_get_sick_p, Post Permissions = none, Pre-Permissions =none
			Method Name = sim_village
			Vertex Name = SeqHealth.children, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Hosp.inside, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.hosp, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.inside, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.time_left, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Hosp.free_personnel, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.free_personnel, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.population, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Hosp.assess, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.assess, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.seed, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.sim_convalescence_p, Post Permissions = immutable, Pre-Permissions =immutable
			Vertex Name = Village.sim_realloc_p, Post Permissions = immutable, Pre-Permissions =immutable
			Vertex Name = Village.level, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.sim_level, Post Permissions = immutable, Pre-Permissions =immutable
			Vertex Name = Village.sim_convalescence_time, Post Permissions = immutable, Pre-Permissions =immutable
			Vertex Name = Village.time, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.root, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Hosp.waiting, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.waiting, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.sim_assess_time, Post Permissions = immutable, Pre-Permissions =immutable
			Vertex Name = Hosp.realloc, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.realloc, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.id, Post Permissions = pure, Pre-Permissions =pure
			Vertex Name = Village.hosps_visited, Post Permissions = share, Pre-Permissions =share
			Vertex Name = Village.sim_get_sick_p, Post Permissions = immutable, Pre-Permissions =immutable
			Class Name = Hosp
			Class Name = SeqHealth
			Method Name = main
			Vertex Name = Patient.time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Patient.time_left, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Patient.hosps_visited, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.children, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.population, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.hosp, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.DEFAULT_THRESHOLD, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_level, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_cities, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_population_ratio, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_assess_time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_convalescence_time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_seed, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_get_sick_p, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_convalescence_p, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_realloc_p, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_population, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_hospitals, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_checkin, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_village, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_waiting, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_assess, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_inside, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.res_avg_stay, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.IQ, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.id, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.level, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.seed, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.root, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.sim_id, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.home_village, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.population, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.hosp, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Health.free_personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Health.children, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.id, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.level, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.seed, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.root, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.hosps_visited, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.time_left, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.time, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.home_village, Post Permissions = none, Pre-Permissions =none
			Vertex Name = SeqHealth.children, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Hosp.inside, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.inside, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Hosp.free_personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.free_personnel, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Hosp.assess, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.assess, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.sim_convalescence_p, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.sim_realloc_p, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.sim_level, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.sim_convalescence_time, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Hosp.waiting, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.waiting, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.sim_assess_time, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Hosp.realloc, Post Permissions = unique, Pre-Permissions =none
			Vertex Name = Village.realloc, Post Permissions = none, Pre-Permissions =none
			Vertex Name = Village.sim_get_sick_p, Post Permissions = none, Pre-Permissions =none*/
	
}
