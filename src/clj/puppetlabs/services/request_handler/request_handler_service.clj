(ns puppetlabs.services.request-handler.request-handler-service
  (:require [puppetlabs.trapperkeeper.core :as tk]
            [puppetlabs.services.protocols.request-handler :as handler]
            [puppetlabs.services.request-handler.request-handler-core :as request-handler-core]
            [puppetlabs.trapperkeeper.services :as tk-services]
            [clojure.tools.logging :as log]))

(tk/defservice request-handler-service
  handler/RequestHandlerService
  [[:PuppetServerConfigService get-config]]
  (init [this context]
    (let [jruby-service (tk-services/get-service this :JRubyPuppetService)
          config (get-config)]
      (when (contains? (:master config) :allow-header-cert-info)
        (if (true? (get-in config [:jruby-puppet :use-legacy-auth-conf]))
          (log/warn "The 'master.allow-header-cert-info' setting is deprecated. "
                    "Remove it, set 'jruby-puppet.use-legacy-auth-conf' to"
                    "'false', migrate your authorization rule definitions in"
                    "the /etc/puppetlabs/puppet/auth.conf file to the"
                    "/etc/puppetlabs/puppetserver/conf.d/auth.conf file, and set"
                    "'authorization.allow-header-cert-info' to the desired value.")
          (log/warn "The 'master.allow-header-cert-info' setting is deprecated"
                    "and will be ignored in favor of the"
                    "'authorization.allow-header-cert-info' setting because"
                    "the 'jruby-puppet.use-legacy-auth-conf' setting is 'false'. "
                    "Remove the 'master.allow-header-cert-info' setting.")))
      (assoc context :request-handler
                     (request-handler-core/build-request-handler
                      jruby-service
                      (request-handler-core/config->request-handler-settings
                       config)))))
  (handle-request
    [this request]
    (let [handler (:request-handler (tk-services/service-context this))]
      (handler request))))
