openssl ecparam -list_curves

openssl ecparam -out secp256r1.pem -name secp256r1 -genkey
openssl req -new -key secp256r1.pem -x509 -sha256 -nodes -days 10000 -out secp256r1.crt
openssl x509 -text -in secp256r1.crt
openssl pkey -in secp256r1.pem -out secp256r1.key

openssl ecparam -out secp384r1.pem -name secp384r1 -genkey
openssl req -new -key secp384r1.pem -x509 -sha384 -nodes -days 10000 -out secp384r1.crt
openssl x509 -text -in secp384r1.crt
openssl pkey -in secp384r1.pem -out secp384r1.key

openssl ecparam -out secp521r1.pem -name secp521r1 -genkey
openssl req -new -key secp521r1.pem -x509 -sha512 -nodes -days 10000 -out secp521r1.crt
openssl x509 -text -in secp521r1.crt
openssl pkey -in secp521r1.pem -out secp521r1.key

openssl ecparam -out ecdsasha1.pem -name secp256r1 -genkey
openssl req -new -key ecdsasha1.pem -x509 -sha1 -nodes -days 10000 -out ecdsasha1.crt
openssl x509 -text -in ecdsasha1.crt
openssl pkey -in ecdsasha1.pem -out ecdsasha1.key

--------------------------------------------------

openssl genrsa -out rsa.key 1024
openssl req -new -key rsa.key -x509 -sha1 -nodes -days 10000 -out rsasha1.crt
openssl x509 -text -in rsasha1.crt

openssl req -new -key rsa.key -x509 -sha256 -nodes -days 10000 -out rsasha256.crt
openssl x509 -text -in rsasha256.crt

openssl req -new -key rsa.key -x509 -sha384 -nodes -days 10000 -out rsasha384.crt
openssl x509 -text -in rsasha384.crt

openssl req -new -key rsa.key -x509 -sha512 -nodes -days 10000 -out rsasha512.crt
openssl x509 -text -in rsasha512.crt
openssl pkey -in rsa.key -out ecdsasha1.key1

--------------------------------------------------

openssl genrsa -out rsa2048.pem 2048
openssl req -new -x509 -key rsa2048.pem -days 10000 -sigopt rsa_padding_mode:pss -sha256 -sigopt rsa_pss_saltlen:32 -out rsapsssha256.crt
openssl x509 -text -in rsapsssha256.crt

openssl req -new -x509 -key rsa2048.pem -days 10000 -sigopt rsa_padding_mode:pss -sha384 -sigopt rsa_pss_saltlen:48 -out rsapsssha384.crt
openssl x509 -text -in rsapsssha384.crt

openssl req -new -x509 -key rsa2048.pem -days 10000 -sigopt rsa_padding_mode:pss -sha512 -sigopt rsa_pss_saltlen:64 -out rsapsssha512.crt
openssl x509 -text -in rsapsssha512.crt
openssl pkey -in rsa2048.pem -out rsa2048.key

---------------------------------------------------

openssl genpkey -algorithm rsa-pss -pkeyopt rsa_keygen_bits:2048 -pkeyopt rsa_pss_keygen_md:sha256 -pkeyopt rsa_pss_keygen_mgf1_md:sha256 -pkeyopt rsa_pss_keygen_saltlen:32 -out rsapsssha256.pem
openssl req -new -x509 -key rsapsssha256.pem -days 10000 -sigopt rsa_padding_mode:pss -sha256 -sigopt rsa_pss_saltlen:32 -out rsapsspsssha256.crt
openssl x509 -text -in rsapsspsssha256.crt
openssl pkey -in rsapsssha256.pem -out rsapsssha256.key

openssl genpkey -algorithm rsa-pss -pkeyopt rsa_keygen_bits:2048 -pkeyopt rsa_pss_keygen_md:sha384 -pkeyopt rsa_pss_keygen_mgf1_md:sha384 -pkeyopt rsa_pss_keygen_saltlen:48 -out rsapsssha384.pem
openssl req -new -x509 -key rsapsssha384.pem -days 10000 -sigopt rsa_padding_mode:pss -sha384 -sigopt rsa_pss_saltlen:48 -out rsapsspsssha384.crt
openssl x509 -text -in rsapsspsssha384.crt
openssl pkey -in rsapsssha384.pem -out rsapsssha384.key

openssl genpkey -algorithm rsa-pss -pkeyopt rsa_keygen_bits:2048 -pkeyopt rsa_pss_keygen_md:sha512 -pkeyopt rsa_pss_keygen_mgf1_md:sha512 -pkeyopt rsa_pss_keygen_saltlen:64 -out rsapsssha512.pem
openssl req -new -x509 -key rsapsssha512.pem -days 10000 -sigopt rsa_padding_mode:pss -sha512 -sigopt rsa_pss_saltlen:64 -out rsapsspsssha512.crt
openssl x509 -text -in rsapsspsssha512.crt
openssl pkey -in rsapsssha512.pem -out rsapsssha512.key

--------------------------------------------------

openssl genpkey -algorithm ED25519 -out ed25519.pem
openssl req -new -key ed25519.pem -x509 -nodes -days 10000 -out ed25519.crt
openssl x509 -text -in ed25519.crt
openssl pkey -in ed25519.pem -out ed25519.key

openssl genpkey -algorithm ED448 -out ed448.pem
openssl req -new -key ed448.pem -x509 -nodes -days 10000 -out ed448.crt
openssl x509 -text -in ed448.crt
openssl pkey -in ed448.pem -out ed448.key
