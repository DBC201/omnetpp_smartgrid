This is a fork of OMNeT++ 6.0.3. The original repo along with the source code can be found at: https://github.com/omnetpp/omnetpp/tree/omnetpp-6.0.3

The purpose of this fork is to help simulating smart grid communications for research purposes.

This has been built and tested for Linux OS's with x86_64 architecture. The release version with the ide pre built similar to the releases provided in OMNeT++ is available in the release section of this repository. It can also be downloaded by clicking [here](https://github.com/DBC201/omnetpp_smartgrid/releases/download/52ecc32/omnetpp_smartgrid-linux-x86_64.tgz).

Following changes were made to the original code:
- IDE build was configured for development purposes: [f571189](https://github.com/DBC201/omnetpp_smartgrid/commit/f571189a4858f144fb0be291642409c25cc71535), [c34a543](https://github.com/DBC201/omnetpp_smartgrid/commit/c34a543340d44ebda450a58d2bb1f037daeb396d)
- A hardcoded link to [inet_smartgrid](https://github.com/DBC201/inet_smartgrid) was added so that it can be set up with ease: [e8055b0](https://github.com/DBC201/omnetpp_smartgrid/commit/e8055b00196072b4305e4da9546c82002fbdf604)
- Vector buffer and vector memory limit was increased to support low delay SV packet traffic defined by IEC61850: [38eb53c](https://github.com/DBC201/omnetpp_smartgrid/commit/38eb53cece2c06ec9ca67f20494327962ceada29)

---Below is the continuation to the original readme.---

[![Join the chat at https://gitter.im/omnetpp/community](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/omnetpp/community)

# OMNeT++

OMNeT++ is a public-source, component-based, modular and open-architecture
simulation environment with strong GUI support and an embeddable simulation
kernel. Its primary application area is the simulation of communication
networks, but it has been successfully used in other areas like the simulation
of IT systems, queueing networks, hardware architectures and business processes
as well.

See the main OMNeT++ website [omnetpp.org](https://omnetpp.org) for documentation,
tutorials and other introductory materials, release downloads, model catalog,
and other useful information.

## License

OMNeT++ is distributed under the [Academic Public License](../doc/License).

## Installation

To compile OMNeT++ after cloning the repository, first create your local copy
of the `configure.user` file:

    cd omnetpp
    cp configure.user.dist configure.user

then follow instructions in the [Installation
Guide](https://doc.omnetpp.org/omnetpp/InstallGuide.pdf) of the last release.
It contains OS-specific information on installing dependencies, selecting build
options, compilation, and more. (The hyperlinked PDF is generated from the
sources in `doc/src/installguide`.)
