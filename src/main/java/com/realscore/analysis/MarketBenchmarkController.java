package com.realscore.analysis;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only market benchmarks, computed on each request. */
@RestController
@RequiredArgsConstructor
class MarketBenchmarkController {

    private final MarketBenchmarkService marketBenchmarkService;

    @GetMapping("/api/analysis/benchmarks/sale-price-per-m2")
    List<SalePriceBenchmark> salePricePerM2() {
        return marketBenchmarkService.salePricePerM2();
    }

    @GetMapping("/api/analysis/benchmarks/rent")
    List<RentBenchmark> rent() {
        return marketBenchmarkService.rent();
    }
}
