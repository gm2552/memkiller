package com.java.example.memkiller.resources;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("memory")
public class MemoryResource 
{

	protected final Map<String, ExecutorService> services = new ConcurrentHashMap<>();
	
	protected final Map<String, MemJob> jobs = new ConcurrentHashMap<>();
	
	@GetMapping
	public Mono<Long> getMemoryConsumed()
	{
		var memUsed = 0L;
		
		for (MemJob job : jobs.values())
			memUsed += job.getMemUsed();
		
		return Mono.just(memUsed);
	}
	
	@PostMapping("consumer")
	public Mono<String> submitConsumptionJob(@RequestParam(name="allocSize") int allocSize, 
			@RequestParam(name="allocInterval") int allocInterval)
	{
		final var jobId = UUID.randomUUID().toString();
		
		final ScheduledExecutorService executorService
	      = Executors.newSingleThreadScheduledExecutor();
		
		final var job = new MemJob(allocSize);
		
		executorService.scheduleAtFixedRate(job, 0, allocInterval, TimeUnit.SECONDS);
		
		services.put(jobId, executorService);
		jobs.put(jobId, job);
		
		return Mono.just(jobId);
	}
	
	protected class MemJob implements Runnable
	{
		protected final List<byte[]> memChunks = Collections.synchronizedList(new LinkedList<byte[]>());
		
		protected final int allocSize;
		
		public MemJob(int allocSize)
		{
			this.allocSize = allocSize;
		}
		
		@Override
		public void run() 
		{
			
			memChunks.add(new byte[allocSize]);
		}
		
		public long getMemUsed()
		{
			return allocSize * memChunks.size();
		}

	}
}


