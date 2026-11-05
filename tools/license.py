# pip install cryptography
# python tools/license.py genkey            -> buat kunci (sekali saja), tampilkan kunci publik
# python tools/license.py issue XXXX-XXXX-XXXX -> terbitkan kode lisensi untuk ID perangkat
import sys, os, base64
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives import hashes, serialization

P = "private.pem"
a = sys.argv[1:]
if not a:
    print("pemakaian: license.py genkey | issue <ID-PERANGKAT>")
elif a[0] == "genkey":
    if os.path.exists(P):
        print("private.pem sudah ada, tidak ditimpa")
    else:
        k = ec.generate_private_key(ec.SECP256R1())
        open(P, "wb").write(k.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
        pub = k.public_key().public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo)
        print("Tempel ke License.kt (PUB):")
        print(base64.b64encode(pub).decode())
        print("JAGA private.pem, jangan di-upload ke GitHub. Hilang = tidak bisa menerbitkan lisensi.")
elif a[0] == "issue" and len(a) > 1:
    k = serialization.load_pem_private_key(open(P, "rb").read(), None)
    sig = k.sign(("SMTV1|" + a[1].strip().upper()).encode(), ec.ECDSA(hashes.SHA256()))
    print(base64.urlsafe_b64encode(sig).rstrip(b"=").decode())
