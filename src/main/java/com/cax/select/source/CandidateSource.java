package com.cax.select.source;

import java.util.Iterator;

// lazy pull

public interface CandidateSource {
    Iterator<Candidate> iterator();
}
