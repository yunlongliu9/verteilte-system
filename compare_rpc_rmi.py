import sys
import os

import pandas as pd
import matplotlib.pyplot as plt


def main():
    csv_path = os.path.join(os.path.dirname(__file__), 'output.csv')

    df = pd.read_csv(csv_path)
    # Ensure correct dtypes
    df['auctionCount'] = df['auctionCount'].astype(int)
    df['callDurationMicros'] = df['callDurationMicros'].astype(float)
    df['mode'] = df['mode'].astype(str)

    output_path = f'rpc_vs_rmi.png'

    # compute mean per mode and auctionCount
    stats = df.groupby(['mode', 'auctionCount'])['callDurationMicros'].agg(['mean']).reset_index()
    modes = stats['mode'].unique()

    plt.figure(figsize=(10,6))
    for mode in modes:
        m = stats[stats['mode'] == mode]
        plt.plot(m['auctionCount'], m['mean'], label=mode, marker='o')

    plt.xlabel('Number of Auctions')
    plt.ylabel('Mean Call Duration (µs)')
    plt.title('Mean Latency vs Number of Auctions')
    plt.legend()
    plt.grid(alpha=0.3)
    plt.tight_layout()
    plt.savefig(output_path)
    plt.close()

if __name__ == "__main__":
    main()
