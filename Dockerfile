FROM php:8.2-apache

LABEL org.opencontainers.image.title="Pinterest J2ME Proxy" \
      org.opencontainers.image.description="Self-hosted Pinterest JSON and image proxy for the Pinterest J2ME MIDlet" \
      org.opencontainers.image.source="https://github.com/mrlima4095/Pinterest-J2ME" \
      org.opencontainers.image.licenses="MIT"

ENV PROXY_ALLOWED_HOSTS=pinimg.com \
    PROXY_MAX_BYTES=8388608

RUN a2enmod headers expires \
    && printf '%s\n' 'ServerName localhost' 'ServerTokens Prod' 'ServerSignature Off' \
        > /etc/apache2/conf-available/security.conf \
    && a2enconf security \
    && rm -rf /var/www/html/*

COPY index.php pinterest.php proxy.php /var/www/html/
COPY docker/healthcheck.sh /usr/local/bin/pinterest-healthcheck
RUN chmod 0755 /usr/local/bin/pinterest-healthcheck \
    && chown -R www-data:www-data /var/www/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD ["/usr/local/bin/pinterest-healthcheck"]
