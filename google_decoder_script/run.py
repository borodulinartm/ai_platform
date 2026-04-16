from googlenewsdecoder import gnewsdecoder
import sys

def main():
    interval_time = 1  # interval is optional, default is None

    source_url = sys.argv[1]

    try:
        decoded_url = gnewsdecoder(source_url, interval=interval_time)

        if decoded_url.get("status"):
            print(decoded_url["decoded_url"])
        else:
            print("Error:", decoded_url["message"])
    except Exception as e:
        print(f"Error occurred: {e}")

if __name__ == "__main__":
    main()
