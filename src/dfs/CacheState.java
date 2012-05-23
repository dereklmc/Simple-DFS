package dfs;

public enum CacheState {
	INVALID {
		@Override
		public CacheState readFile() {
			// TODO Auto-generated method stub
			return READ_SHARED;
		}

		@Override
		public CacheState writeFile() {
			// TODO Auto-generated method stub
			return WRITE_OWNED;
		}
	},
	READ_SHARED {
		@Override
		public CacheState readFile() {
			// TODO Auto-generated method stub
			return READ_SHARED;
		}

		@Override
		public CacheState writeFile() {
			// TODO Auto-generated method stub
			return WRITE_OWNED;
		}
	},
	WRITE_OWNED {
		@Override
		public CacheState readFile() {
			// TODO Auto-generated method stub
			return WRITE_OWNED;
		}

		@Override
		public CacheState writeFile() {
			// TODO Auto-generated method stub
			return WRITE_OWNED;
		}
	},
	RELEASE_OWNERSHIP {
		@Override
		public CacheState readFile() {
			// TODO Auto-generated method stub
			return RELEASE_OWNERSHIP;
		}

		@Override
		public CacheState writeFile() {
			// TODO Auto-generated method stub
			return RELEASE_OWNERSHIP;
		}
	};
	
	public abstract CacheState readFile();
	public abstract CacheState writeFile();
}
