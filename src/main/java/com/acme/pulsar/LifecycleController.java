package com.acme.pulsar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultLifecycleProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lc")
@ResponseStatus(HttpStatus.OK)
class LifecycleController {

	@Autowired
	private DefaultLifecycleProcessor lifecycleProcessor;

	@GetMapping("/stop")
	String stop() {
		try {
			this.lifecycleProcessor.stop();
		} catch (Exception ex) {
			ex.printStackTrace();
			return "FAILED TO STOP: " + ex.getMessage();
		}
		return "STOPPED";
	}

	@GetMapping("/stopForRestart")
	String stopForRestart() {
		try {
			var method = ReflectionUtils.findMethod(DefaultLifecycleProcessor.class, "stopForRestart");
			ReflectionUtils.makeAccessible(method);
			ReflectionUtils.invokeMethod(method, this.lifecycleProcessor);
		} catch (Exception ex) {
			ex.printStackTrace();
			return "FAILED TO STOP FOR RESTART: " + ex.getMessage();
		}
		var restarter = new Thread(() -> {
			try { Thread.sleep(20_000); } catch (InterruptedException e) { }
			System.out.println("*** Restaring...");
			restart();
		});
		restarter.setDaemon(false);
		restarter.start();

		return "STOPPED-FOR-RESTART";
	}

	//@GetMapping("/restartAfterStop")
	void restart() {
		try {
			var method = ReflectionUtils.findMethod(DefaultLifecycleProcessor.class, "restartAfterStop");
			ReflectionUtils.makeAccessible(method);
			ReflectionUtils.invokeMethod(method, this.lifecycleProcessor);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("*** FAILED TO RESTART AFTER STOP: " + ex.getMessage());
		}
	}
}
