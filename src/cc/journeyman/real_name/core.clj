(ns cc.journeyman.real-name.core
  "Resolve real names from user names in a platform independent fashion."
  (:require [clojure.java.io :refer [reader]]
            [clojure.java.shell :refer [sh]]
            [clojure.string :refer [split starts-with? trim]])
  (:gen-class))

(defn- mac-os-x
  "Mac OS X, use the `id -F` command."
  [username]
  (let [response (sh "id" "-F" username)]
    (if (zero? (:exit response))
      (trim (:out response))
      (throw (ex-info
              (format "Real name for `%s` not found because `%s`."
                      username
                      (trim (:err response)))
              {:username username
               :error (:err response)
               :os-name (System/getProperty "os.name")
               :os-version (System/getProperty "os.version")})))))

(defn delimited-record->map
  "Split this `record` into fields delimited by this `delimiter-pattern` and
   return it as a map with these `keys`."
  [^String record ^java.util.regex.Pattern delimiter-pattern keys]
  (apply assoc
         (cons {} (interleave keys (split record  delimiter-pattern)))))

(defn process-gecos
  "Process this `gecos` field into a map of its sub-fields. See
   https://en.wikipedia.org/wiki/Gecos_field"
  [gecos]
  (delimited-record->map gecos #"," [:real-name :address :work-phone :home-phone :other]))

(defn process-passwd-line
  "Process this `line` from a passwd file"
  [line]
  (let [record (delimited-record->map line #":" [:uname :pass :uid :gid :gecos :sh])]
    (when record (assoc record :gecos (process-gecos (:gecos record))))))

(defn process-passwd
  "Process the password file into a map whose keys are user names and whose
   values are maps of associated records from lines in the file."
  []
  (reduce #(assoc %1 (:uname %2) %2)
          {}
          (map process-passwd-line 
               (remove #(starts-with? % "#")(line-seq (reader "/etc/passwd"))))))

(defn- unix
  "Generic unix, parse the GECOS field of the passwd record matching `username`."
  ([username]
   (let [real-name (-> ((process-passwd) username) :gecos :real-name)]
     (if real-name
       real-name
       (throw (ex-info (format "Real name for `%s` not found." username)
                       {:username username
                        :os-name (System/getProperty "os.name")
                        :os-version (System/getProperty "os.version")}))))))

(defn- windows7+
  "Very experimental, probably wrong."
  [username]
  (let [response (sh "net" "user" username "/domain" "|" "FIND" "/I" "Full Name")]
    (if (zero? (:exit response))
      (trim (:out response))
      (throw (ex-info
              (format "Real name for `%s` not found" username)
              {:username username
               :response response
               :os-name (System/getProperty "os.name")
               :os-version (System/getProperty "os.version")})))))

(defn username->real-name
  "Given this `username`, return the associated real name if found; else
   throw exception."
  [^String username]
  (let [os-name (System/getProperty "os.name")]
    (case os-name
      ("AIX" "FreeBSD" "Irix" "Linux" "Solaris") (unix username)
      "Mac OS X" (mac-os-x username)
    ;; "Windows 95"
    ;; "Windows NT"
    ;; "Windows Vista" 
      ("Windows 7"
       "Windows 8"
       "Windows 8.1"
       "Windows 10"
       "Windows 11") (windows7+ username)
    ;; else
      (throw (ex-info
              (format "No hack available to find real name for user on `%s`."
                      os-name)
              {:username username
               :os-name os-name})))))

(defn get-real-name
  "Get the real name of the user with this `username`, or of the current user 
   if no username passed."
  ([]
   (get-real-name (System/getProperty "user.name")))
  ([^String username]
   (username->real-name username)))

(defn getRealName
  "Get the real name of the user with this `username`, or of the current user 
   if no username passed.
   
   Essentially a wrapper around `get-real-name` for the convenience of Java 
   users."
  ([]
   (get-real-name))
  ([username]
   (get-real-name username)))