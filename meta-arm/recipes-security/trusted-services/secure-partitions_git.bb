SUMMARY = "Trusted Services secure partitions"
HOMEPAGE = "https://trusted-services.readthedocs.io/en/latest/index.html"

COMPATIBLE_MACHINE ?= "invalid"

PACKAGE_ARCH = "${MACHINE_ARCH}"

LICENSE = "Apache-2.0 & BSD-3-Clause & Zlib"
LIC_FILES_CHKSUM = "file://license.rst;md5=ea160bac7f690a069c608516b17997f4 \
                    file://../mbedcrypto/LICENSE;md5=302d50a6369f5f22efdb674db908167a \
                    file://../nanopb/LICENSE.txt;md5=9db4b73a55a3994384112efcdb37c01f"

SRC_URI = "git://git.trustedfirmware.org/TS/trusted-services.git;protocol=https;branch=integration;name=ts;destsuffix=git/ts ${SRC_URI_MBED} ${SRC_URI_NANOPB}"

SRC_URI_MBED = "git://github.com/ARMmbed/mbed-crypto.git;protocol=https;branch=development;name=mbed;destsuffix=git/mbedcrypto"
SRC_URI_NANOPB = "git://github.com/nanopb/nanopb.git;name=nanopb;protocol=https;branch=master;destsuffix=git/nanopb"

SRCREV_FORMAT = "ts"
SRCREV_ts = "c52807cfea6edab5d5c9cc0cfdb18ffe12cfdb0c"
SRCREV_mbed = "cf4a40ba0a3086cabb5a8227245191161fd26383"
SRCREV_nanopb = "df0e92f474f9cca704fe2b31483f0b4d1b1715a4"
PV = "0.0+git${SRCPV}"

# Which environment to create the secure partions for (opteesp or shim)
TS_ENVIRONMENT ?= "opteesp"
S = "${WORKDIR}/git/ts"
B = "${WORKDIR}/build"

inherit deploy python3native

DEPENDS = "python3-pycryptodome-native python3-pycryptodomex-native \
           python3-pyelftools-native python3-grpcio-tools-native \
           python3-protobuf-native protobuf-native cmake-native \
           "

DEPENDS:append = " ${@bb.utils.contains('TS_ENVIRONMENT', 'opteesp', 'optee-spdevkit', '', d)}"

EXTRA_OEMAKE += "HOST_PREFIX=${HOST_PREFIX}"
EXTRA_OEMAKE += "CROSS_COMPILE64=${HOST_PREFIX}"

export CROSS_COMPILE="${TARGET_PREFIX}"

CFLAGS[unexport] = "1"
CPPFLAGS[unexport] = "1"
AS[unexport] = "1"
LD[unexport] = "1"

# setting the linker options used to build the secure partitions
SECURITY_LDFLAGS = ""
TARGET_LDFLAGS = "-Wl,--build-id=none -Wl,--hash-style=both"

# only used if TS_ENVIRONMENT is opteesp
SP_DEV_KIT_DIR = "${@bb.utils.contains('TS_ENVIRONMENT', 'opteesp', '${STAGING_INCDIR}/optee/export-user_sp', '', d)}"

# SP images are embedded into optee os image
SP_PACKAGING_METHOD ?= "embedded"

do_configure[cleandirs] = "${B}"

do_configure() {
    for TS_DEPLOYMENT in ${TS_DEPLOYMENTS}; do
        cmake \
          -DCMAKE_INSTALL_PREFIX=${D}/firmware/sp \
          -DSP_DEV_KIT_DIR=${SP_DEV_KIT_DIR} \
          -DSP_PACKAGING_METHOD=${SP_PACKAGING_METHOD} \
          -S ${S}/$TS_DEPLOYMENT -B "${B}/$TS_DEPLOYMENT"
    done
}

do_compile() {
    for TS_DEPLOYMENT in ${TS_DEPLOYMENTS}; do
        cmake --build "${B}/$TS_DEPLOYMENT"
    done
}

do_install () {
    if [ "${TS_ENVIRONMENT}" = "opteesp" ]; then
        for TS_DEPLOYMENT in ${TS_DEPLOYMENTS}; do
            cmake --install "${B}/$TS_DEPLOYMENT"
        done
    fi
}

SYSROOT_DIRS = "/firmware"

do_deploy() {
    cp -rf ${D}/firmware/* ${DEPLOYDIR}/
}
addtask deploy after do_install

FILES:${PN} = "/firmware/*"