# runs all maps for two bots

import os

# these constants are probably fine
maps = ['AllElements', 'DefaultMap', 'SmallElements', 'maptestsmall']


# set these
teamA = 'sphere'
teamB = 'oneeleven'

def get_winner(data):
    return data[-6].split(' ')[-4]

def run_matches(teamA, teamB, score):
    for map in maps:
        result = os.popen('gradlew run -Pmaps={map} -PteamA={a} -PteamB={b} -Pdebug=false -Penableprofiler=false -PoutputVerbose=false -PshowIndicators=false'.format(map = map, a = teamA, b = teamB)).read().splitlines()
        winner = get_winner(result)[1:2]
        if winner == 'A':
            score[0] += 1
        elif winner == 'B':
            score[1] += 1
        else:
            print('I don\'t know who won this, take a look: {}'.format(winner))
    return score

def main():
    os.chdir('..')
    os.system('gradlew update')
    os.system('gradlew build')

    score1 = run_matches(teamA, teamB, [0, 0])
    print('Match 1 score ({} vs {}): {}'.format(teamA, teamB, score1))
    score2 = run_matches(teamB, teamA, [0, 0])
    print('Match 2 score ({} vs {}): {}'.format(teamB, teamA, score2))
    final_score = [score1[0] + score2[1], score1[1] + score2[0]]
    print('Final score ({} vs {}): {}'.format(teamA, teamB, final_score))

if __name__ == '__main__':
    main()