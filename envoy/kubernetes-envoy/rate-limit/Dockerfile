FROM centos

# MAINTAINER
MAINTAINER zhihui.wu@hpe.com

# put ratelimt into image
ADD ratelimit /bin
#RUN apk add --update bash
ENTRYPOINT ["/bin/ratelimit"]

# docker build -t mycat/ratelimit .
