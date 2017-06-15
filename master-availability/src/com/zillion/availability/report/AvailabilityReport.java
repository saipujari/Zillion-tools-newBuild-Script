package com.zillion.availability.report;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

import com.zillion.availability.model.TimeBlock;
import com.zillion.availability.model.TimeBlock.BlockType;
import com.zillion.availability.model.TimeSegment;

public class AvailabilityReport {

	// process available timeblocks
	public static TreeSet< Long > getCoachAvailability( String coachId, ArrayList< TimeBlock > availableTimeblocks ) {
		TreeSet< Long > coachAvailability = new TreeSet<Long>();
		
		//System.out.println( "Processing available timeblocks for coachId=" + coachId );
		for( TimeBlock timeblock: availableTimeblocks ) {
			if( timeblock.blockType != BlockType.AVAILABLE ) {
				System.out.println( "ERROR - wrong block type" );
			}
			
			Long currentSegmentStart = timeblock.startTime;
			Long currentSegmentEnd = timeblock.startTime + segmentDuration;
			
			while( currentSegmentEnd <= timeblock.endTime ) {
				coachAvailability.add( currentSegmentStart );
				//System.out.println(new Date(currentSegmentStart));
				currentSegmentStart = currentSegmentEnd;
				currentSegmentEnd += segmentDuration;
			}
		}
		
		return coachAvailability;
	}
	
	
	// process unavailable timeblocks
	public static TreeSet< Long > removeCoachUnavailability( String coachId, ArrayList< TimeBlock > unavailableTimeblocks, TreeSet< Long > coachAvailability ) {
		TreeSet< Long > coachAvailabilityResult = new TreeSet<Long>( coachAvailability );
		
		//System.out.println( "Processing unavailable timeblocks for coachId=" + coachId );
		for( TimeBlock timeblock: unavailableTimeblocks ) {
			if( timeblock.blockType != BlockType.UNAVAILABLE ) {
				System.out.println( "ERROR - wrong block type" );
			}
			
			Long currentSegmentStart = timeblock.startTime;
			Long currentSegmentEnd = timeblock.startTime + segmentDuration;
			
			while( currentSegmentEnd <= timeblock.endTime ) {
				coachAvailabilityResult.remove( currentSegmentStart );
				//System.out.println(new Date(currentSegmentStart));
				currentSegmentStart = currentSegmentEnd;
				currentSegmentEnd += segmentDuration;
			}
		}
		
		return coachAvailabilityResult;
	}	
	
	static Integer segmentDurationMins = 5;
	static Integer segmentDuration = segmentDurationMins*60*1000;

	public static TreeMap< Long, TimeSegment > getCoachAvailableSessions( String coachId, Long sessionLenMins, TreeSet< Long > coachAvailabilitySegments ) {
		//System.out.println( "Getting available sessions for coachId=" + coachId + " for session length (Mins): " + sessionLenMins );

		TreeMap< Long, TimeSegment > availableSessions = new TreeMap<Long, TimeSegment>();
		
		Long consecutiveSegmentsNeeded = sessionLenMins / segmentDurationMins ;
		Integer segmentsFound = 0;
		Long cumulativeSegmentStart = 0L;
		Long cumulativeSegmentEnd = 0L;
		
		for( Long segmentStart: coachAvailabilitySegments ) {
			//System.out.println("Segment Start : " + segmentStart + " " + new Date(segmentStart));
			// start a new segment ?
			if( segmentStart.compareTo(cumulativeSegmentEnd)!=0 ) {
				segmentsFound = 1;
				cumulativeSegmentStart = segmentStart;
				cumulativeSegmentEnd = segmentStart + segmentDuration;
				//System.out.println("Cumulative Step #1 : " + new Date(cumulativeSegmentStart) + " " + new Date (cumulativeSegmentEnd) + " " + cumulativeSegmentStart + " " + cumulativeSegmentEnd);
			} else {
				// append to current segment
				segmentsFound++;
				cumulativeSegmentEnd += segmentDuration;
				//System.out.println("Cumulative Step #2 : " + new Date(cumulativeSegmentStart) + " " + new Date (cumulativeSegmentEnd));
				
				// if enough consecutive segments, then log another available session
				if( segmentsFound >= consecutiveSegmentsNeeded ) {
					TimeSegment timeSegment = new TimeSegment();
					timeSegment.startTime = cumulativeSegmentStart;
					timeSegment.endTime = cumulativeSegmentEnd;
					timeSegment.durationMins = (cumulativeSegmentEnd-cumulativeSegmentStart) / (60*1000);
					availableSessions.put( timeSegment.startTime, timeSegment );
					//System.out.println("Cumulative Step #3 : " + new Date(timeSegment.startTime) + " " + new Date (timeSegment.endTime));
					
					// reset
					segmentsFound = 0;
					cumulativeSegmentStart = 0L;
					cumulativeSegmentEnd = 0L;
					
				}
			}
		}

		return availableSessions;
	}
	
}
