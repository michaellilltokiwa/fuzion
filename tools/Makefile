
firefox-profiler:
	docker build -t ffp -f firefox-profiler.Dockerfile .
	docker run -v "$(PWD)/webpack.local-config.js:/app/webpack.local-config.js:ro" -p 4242:4242 ffp

perfetto:
	docker build -t pf -f perfetto.Dockerfile .
	docker run -p 10000:10000 pf

speedscope:
	docker build -t speedscope -f speedscope.Dockerfile .
	docker run -p 8000:8000 speedscope

hotspot:
	docker build -t hotspot -f hotspot.Dockerfile .
	docker run --rm -e QT_QPA_PLATFORM=wayland -e WAYLAND_DISPLAY=$$WAYLAND_DISPLAY -v $$XDG_RUNTIME_DIR/$$WAYLAND_DISPLAY:/tmp/$$WAYLAND_DISPLAY -v $$XDG_RUNTIME_DIR:$$XDG_RUNTIME_DIR -v /run/user/$$(id -u)/wayland-0:/run/user/$$(id -u)/wayland-0 -e XDG_RUNTIME_DIR=$$XDG_RUNTIME_DIR -u $$(id -u):$$(id -g) -v perf.data:/perf.data --device /dev/dri hotspot

