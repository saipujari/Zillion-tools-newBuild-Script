package com.zillion.availability.model;

public class TimeBlock {
	
	public enum BlockType { AVAILABLE, UNAVAILABLE }

	public Long startTime;
	public Long endTime;
		
	public BlockType blockType;

	public Long getStartTime() {
		return startTime;
	}

	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}

	public Long getEndTime() {
		return endTime;
	}

	public void setEndTime(Long endTime) {
		this.endTime = endTime;
	}

	public BlockType getBlockType() {
		return blockType;
	}

	public void setBlockType(BlockType blockType) {
		this.blockType = blockType;
	}
	
}