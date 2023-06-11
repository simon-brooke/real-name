(ns cc.journeyman.real-name.core
  "Resolve real names from user names in a platform independent fashion."
  (:require [clojure.java.io :refer [reader]]
            [clojure.java.shell :refer [sh]]
            [clojure.string :refer [lower-case split starts-with? trim]])
  (:import [java.io StringReader]
           [java.util Locale])
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
  (delimited-record->map (str gecos ",?") #"," [:real-name :address :work-phone :home-phone :other]))

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

(def full-name-in-locale
  "Microsoft's own translations of `Full Name` into a variety of languages,
   taken from their language portal. Clearly there must be some os-call that
   they're using to resolve the translation, but I don't know it; so this is
   the best I can currently do."
  {"ar" "الاسم الكامل"
   "bg" "Пълно име"
   "ca" "Nom complet"
   "cs" "Celé jméno"
   "da" "Fulde navn"
   "de" "Vollständiger Name"
   "el" "Ονοματεπώνυμο" ;; Greek
   "en" "Full Name"
   "es" "Nombre completo"
   "et" "Täielik nimi" ;; Microsoft also uses 'Täisnimi' in some contexts!
   "fi" "Koko nimi"
   "fr" "Nom complet"
   "ga" "Ainm agus Sloinne" ;; Microsoft also uses 'Ainm iomlán' in some contexts!
   "hu" "Teljes név"
   "is" "Fullt Nafn"
   "it" "Nome completo"
   "jp" "氏名" ;; Microsoft also uses 'フル ネーム' and '完全名' in some contexts!
   "lt" "Vardas, pavardė" ;; or 'Visas pavadinimas', or 'Visas vardas', or 'vardas ir pavardė'!
   "lv" "Pilns vārds" ;; or 'Pilnais vārds', or 'Pilns nosaukums'
   "nb" "Fullt navn" ;; Norse
   "nl" "Volledige naam"
   "nn" "Fullt namn" ;; also Norse
   "no" "Fullt namn" ;; also Norse
   "pl" "Imię i nazwisko" ;; or 'Pełna nazwa'
   "pt" "Nome completo"
   "ro" "Nume complet"
   "ru" "Полное название" ;; or 'ФИО', or 'Полное имя'
   "se" "Fullständigt namn"
   "sk" "Celé meno"
   "sl" "Polno ime" ;; or 'ime in priimek'
   "tr" "Tam Adı" ;; or 'Tam Ad', or 'Adı-soyadı', or 'Ad ve soyadı', or 'Adı ve Soyadı'
   "uk" "Повне ім’я" ;; or 'Ім'я та прізвище'
   "zh" "全名"})

(defn- windows7+
  "Very experimental, probably wrong, certainly works only in selected
   locales."
  [username]
  (let [response (sh "net" "user" username)
        full-name (full-name-in-locale
                   (first (split (str (Locale/getDefault)) #"_")))]
    (if (zero? (:exit response))
      (trim
       (subs
        (first (filter
                ;; Cast to lower-case because although in English,
                ;; Microsoft uses the capitalisation 'Full Name',
                ;; according to their own language portal in other
                ;; languages they don't; and I don't trust them!
                #(starts-with? (lower-case %) (lower-case full-name))
                (line-seq (reader (StringReader. (:out response))))))
        (count full-name)))
      (throw (ex-info
              (format "Real name for `%s` not found" username)
              {:username username
               :response response
               :locale (Locale/getDefault)
               :localised-key full-name
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
