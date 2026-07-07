package de.zorro909.codecheck.core.coverage;

public interface CoverageService {

    CoverageSnapshot currentCoverage(CoverageRequest request);

    CoverageFreshness freshness(CoverageRequest request);

    CoverageSnapshot refreshCoverage(CoverageRequest request);

}
