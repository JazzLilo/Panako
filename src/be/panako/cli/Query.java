/***************************************************************************
*                                                                          *
* Panako - acoustic fingerprinting                                         *
* Copyright (C) 2014 - 2017 - Joren Six / IPEM                             *
*                                                                          *
* This program is free software: you can redistribute it and/or modify     *
* it under the terms of the GNU Affero General Public License as           *
* published by the Free Software Foundation, either version 3 of the       *
* License, or (at your option) any later version.                          *
*                                                                          *
* This program is distributed in the hope that it will be useful,          *
* but WITHOUT ANY WARRANTY; without even the implied warranty of           *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
* GNU Affero General Public License for more details.                      *
*                                                                          *
* You should have received a copy of the GNU Affero General Public License *
* along with this program.  If not, see <http://www.gnu.org/licenses/>     *
*                                                                          *
****************************************************************************
*    ______   ________   ___   __    ________   ___   ___   ______         *
*   /_____/\ /_______/\ /__/\ /__/\ /_______/\ /___/\/__/\ /_____/\        *
*   \:::_ \ \\::: _  \ \\::\_\\  \ \\::: _  \ \\::.\ \\ \ \\:::_ \ \       *
*    \:(_) \ \\::(_)  \ \\:. `-\  \ \\::(_)  \ \\:: \/_) \ \\:\ \ \ \      *
*     \: ___\/ \:: __  \ \\:. _    \ \\:: __  \ \\:. __  ( ( \:\ \ \ \     *
*      \ \ \    \:.\ \  \ \\. \`-\  \ \\:.\ \  \ \\: \ )  \ \ \:\_\ \ \    *
*       \_\/     \__\/\__\/ \__\/ \__\/ \__\/\__\/ \__\/\__\/  \_____\/    *
*                                                                          *
****************************************************************************
*                                                                          *
*                              Panako                                      *
*                       Acoustic Fingerprinting                            *
*                                                                          *
****************************************************************************/






package be.panako.cli;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import be.panako.strategy.QueryResult;
import be.panako.strategy.QueryResultHandler;
import be.panako.strategy.Strategy;
import be.panako.util.Config;
import be.panako.util.Key;


/**
 * Query the storage for audio fragments.
 * @author Joren Six
 */
public class Query extends Application{
	private final static Logger LOG = Logger.getLogger(Query.class.getName());

	@Override
	public void run(String... args) {
		int processors = availableProcessors();
		List<File> files = this.getFilesFromArguments(args);
		if(files.size() > 1){
			System.out.println("Processing " + files.size() + " queries on " + processors + " seperate threads.");
		}
		
		Panako.printQueryResultHeader();
		
		if(hasArgument("debug", args) || processors==1){
			System.err.println("Debug argument detected");
			for(File file: files){
				new QueryTask(file.getPath()).run();
			}
		}else{
			ExecutorService executor = Executors.newFixedThreadPool(processors);
			for(File file: files){
				executor.submit(new QueryTask(file.getPath()));
			}
			executor.shutdown();
			try {
				//wait for tasks to finish
				executor.awaitTermination(300, java.util.concurrent.TimeUnit.DAYS);
				System.exit(0);
			} catch (InterruptedException e1) {
				//Thread was interrupted
				LOG.severe("Did not finish all tasks, thread was interrupted!");
			}
		}
		
		
		
	}

	@Override
	public String description() {
		return "Calculates fingerprints for the audio query and matches those with the database.";
	}

	@Override
	public String synopsis() {
		return "[audio_file...]";
	}

	private static class QueryTask implements Runnable, QueryResultHandler{
		private final String path;
		private final HashSet<Integer> emptyHashSet = new HashSet<Integer>();
		private final Strategy strategy;
		private final int numberOfQueryResults;
		
		public QueryTask(String path){
			this.path = path;
			this.numberOfQueryResults = Config.getInt(Key.NUMBER_OF_QUERY_RESULTS);
			strategy = Strategy.getInstance();
		}

		@Override
		public void run() {
			
			strategy.query(path, this.numberOfQueryResults,emptyHashSet, this);
		}
		
		@Override
		public void handleQueryResult(QueryResult r) {
			Panako.printQueryResult(r);
		}

		@Override
		public void handleEmptyResult(QueryResult r) {
			Panako.printQueryResult(r);	
		}
	}	
	
	@Override
	public boolean needsStorage() {
		return true;
	}
	
	@Override
	public boolean writesToStorage() {
		return false;
	}
}
