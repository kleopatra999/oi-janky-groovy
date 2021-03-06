// https://github.com/jenkinsci/pipeline-examples/blob/666e5e3f8104efd090e698aa9b5bc09dd6bf5997/docs/BEST_PRACTICES.md#groovy-gotchas
// tl;dr, iterating over Maps in pipeline groovy is pretty broken in real-world use
archesMeta = [
	//['amd64', [:]],
	//['arm32v5', [:]],
	//['arm32v6', [:]],
	//['arm32v7', [:]],
	//['arm64v8', [:]],
	['i386', [:]],
	['ppc64le', [:]],
	['s390x', [:]],
]
dpkgArches = [
	'amd64': 'amd64',
	'arm32v5': 'armel',
	'arm32v6': 'armhf', // Raspberry Pi, making life hard...
	'arm32v7': 'armhf',
	'arm64v8': 'arm64',
	'i386': 'i386',
	'ppc64le': 'ppc64el',
	's390x': 's390x',
]
apkArches = [
	'amd64': 'x86_64',
	'arm32v6': 'armhf',
	'arm32v7': 'armhf', // Raspberry Pi, making life hard...
	'arm64v8': 'aarch64',
	'i386': 'x86',
	'ppc64le': 'ppc64le',
	's390x': 's390x',
]

def defaultImageMeta = [
	['arches', ['amd64'] as Set],
	['pipeline', 'multiarch/target-generic-pipeline.groovy'],
]

imagesMeta = [:]

// base images (requires custom pipelines for now)
imagesMeta['alpine'] = [
	'arches': [
		// see http://dl-cdn.alpinelinux.org/alpine/edge/main/
		'amd64',
		'arm32v6',
		'arm64v8',
		'i386',
		'ppc64le',
		's390x',
	] as Set,
	'pipeline': 'multiarch/target-alpine-pipeline.groovy',
]
imagesMeta['debian'] = [
	'arches': [
		// see https://www.debian.org/ports/#portlist-released
		// see also https://lists.debian.org/debian-devel-announce/2016/10/msg00008.html ("Release Architectures for Debian 9 'Stretch'")
		'amd64',
		'arm32v5',
		'arm32v7',
		'arm64v8',
		'i386',
		'ppc64le',
		's390x',
	] as Set,
	'pipeline': 'multiarch/target-debian-pipeline.groovy',
]
imagesMeta['ubuntu'] = [
	'arches': [
		// see https://partner-images.canonical.com/core/xenial/current/
		'amd64',
		'arm32v7',
		'arm64v8',
		'i386',
		'ppc64le',
		's390x',
	] as Set,
	'pipeline': 'multiarch/target-ubuntu-pipeline.groovy',
]

// other images (whose "supported arches" lists are normally going to be a combination of their upstream image arches)
imagesMeta['bash'] = [
	'arches': imagesMeta['alpine']['arches'],
]
imagesMeta['buildpack-deps'] = [
	'arches': (imagesMeta['debian']['arches'] + imagesMeta['ubuntu']['arches']),
]
imagesMeta['golang'] = [
	'arches': (imagesMeta['alpine']['arches'] + [
		'amd64',
		'arm32v7', // "armv6l" binaries, but inside debian's "armhf"
		'i386',
		'ppc64le',
		's390x',
	]),
]

// only debian and alpine variants
for (img in [
	'haproxy',
	'memcached',
	'openjdk',
	//'rabbitmq', // TODO figure out erlang-solutions.com repo
	'tomcat',
]) {
	imagesMeta[img] = [
		'arches': (imagesMeta['alpine']['arches'] + imagesMeta['debian']['arches']),
	]
}

// list of arches
arches = []
// list of images
images = []

// given an arch, returns a list of images
def archImages(arch) {
	ret = []
	for (image in images) {
		if (arch in imagesMeta[image]['arches']) {
			ret << image
		}
	}
	return ret as Set
}

for (int i = 0; i < archesMeta.size(); ++i) {
	def arch = archesMeta[i][0]

	arches << arch
}
arches = arches as Set
for (image in imagesMeta.keySet()) {
	def imageMeta = imagesMeta[image]

	// apply "defaultImageMeta" for missing bits
	//   wouldn't it be grand if we could just use "map1 + map2" here??
	//   dat Jenkins sandbox...
	for (int j = 0; j < defaultImageMeta.size(); ++j) {
		def key = defaultImageMeta[j][0]
		def val = defaultImageMeta[j][1]
		if (imageMeta[key] == null) {
			imageMeta[key] = val
		}
	}

	images << image
	imagesMeta[image] = imageMeta
}
images = images as Set

// return "this" (for use via "load" in Jenkins pipeline, for example)
this
