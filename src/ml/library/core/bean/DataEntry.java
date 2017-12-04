package ml.library.core.bean;

public class DataEntry{
	
	private Long rowIndex;
	private String word,smallCategory,bigCategory;
	private Long timeStamp;
	private Boolean marked;
	/**
	 * generates a timestamp immediately
	 * 
	 * @param rowIndex
	 * @param word
	 * @param smallCategory
	 * @param bigCategory
	 */
	public DataEntry(Long rowIndex, String word, String smallCategory, String bigCategory) {
		super();
		this.rowIndex = rowIndex;
		this.word = word;
		this.smallCategory = smallCategory;
		this.bigCategory = bigCategory;
		this.timeStamp = System.currentTimeMillis();
		this.marked = false;
	}
	public Long getRowIndex() {
		return rowIndex;
	}
	public void setRowIndex(Long rowIndex) {
		this.rowIndex = rowIndex;
	}
	public String getWord() {
		return word;
	}
	public void setWord(String word) {
		this.word = word;
	}
	public void setMarked(Boolean how){
		this.marked = how;
	}
	public String getSmallCategory() {
		return smallCategory;
	}
	public void setSmallCategory(String smallCategory) {
		this.smallCategory = smallCategory;
	}
	public String getBigCategory() {
		return bigCategory;
	}
	public Boolean isMarked(){
		return marked;
	}
	public void setBigCategory(String bigCategory) {
		this.bigCategory = bigCategory;
	}
	public Long getTimeStamp() {
		return timeStamp;
	}
	public void markForWrite(){
		this.marked = true;
	}
	public void setTimeStamp(Long timeStamp) {
		this.timeStamp = timeStamp;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((rowIndex == null) ? 0 : rowIndex.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DataEntry other = (DataEntry) obj;
		if (rowIndex == null) {
			if (other.rowIndex != null)
				return false;
		} else if (!rowIndex.equals(other.rowIndex))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "DataEntry [rowIndex=" + rowIndex + ", word=" + word
				+ ", smallCategory=" + smallCategory + ", bigCategory="
				+ bigCategory + "]";
	}
	
	
	
}